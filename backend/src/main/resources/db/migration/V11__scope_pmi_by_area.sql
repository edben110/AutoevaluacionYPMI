ALTER TABLE pmi DROP CONSTRAINT pmi_self_evaluation_id_key;
ALTER TABLE pmi ADD COLUMN area_id UUID REFERENCES area (id);

-- Divide cada plan anterior manteniendo las valoraciones y objetivos originales.
DO $$
DECLARE
    original RECORD;
    first_area UUID;
    another_area RECORD;
    split_id UUID;
BEGIN
    FOR original IN SELECT * FROM pmi ORDER BY id LOOP
        SELECT process.area_id INTO first_area
        FROM prioritary_component priority
        JOIN component_valuation valuation ON valuation.id = priority.valuation_id
        JOIN component component ON component.id = valuation.component_id
        JOIN process process ON process.id = component.process_id
        WHERE priority.pmi_id = original.id
        ORDER BY process.area_id LIMIT 1;

        IF first_area IS NULL THEN
            SELECT id INTO first_area FROM area ORDER BY (state = 'ACTIVE') DESC, id LIMIT 1;
        END IF;
        IF first_area IS NULL THEN
            RAISE EXCEPTION 'No existe un área de catálogo para conservar el PMI %', original.id;
        END IF;
        UPDATE pmi SET area_id = first_area WHERE id = original.id;

        FOR another_area IN
            SELECT DISTINCT process.area_id
            FROM prioritary_component priority
            JOIN component_valuation valuation ON valuation.id = priority.valuation_id
            JOIN component component ON component.id = valuation.component_id
            JOIN process process ON process.id = component.process_id
            WHERE priority.pmi_id = original.id AND process.area_id <> first_area
        LOOP
            split_id := gen_random_uuid();
            INSERT INTO pmi (id, self_evaluation_id, area_id, projection_years, filled_on, created_at, updated_at)
            VALUES (split_id, original.self_evaluation_id, another_area.area_id, original.projection_years,
                    original.filled_on, original.created_at, original.updated_at);
            UPDATE prioritary_component priority SET pmi_id = split_id
            FROM component_valuation valuation
            JOIN component component ON component.id = valuation.component_id
            JOIN process process ON process.id = component.process_id
            WHERE priority.valuation_id = valuation.id AND priority.pmi_id = original.id
                AND process.area_id = another_area.area_id;
        END LOOP;
    END LOOP;
END $$;

-- Una selección antigua aislada en la segunda posición se conserva en la primera.
WITH positions AS (
    SELECT id, row_number() OVER (PARTITION BY pmi_id ORDER BY selected_slot)::INTEGER AS slot
    FROM prioritary_component WHERE selected_slot IS NOT NULL
)
UPDATE prioritary_component priority SET selected_slot = positions.slot
FROM positions WHERE positions.id = priority.id;

ALTER TABLE pmi ALTER COLUMN area_id SET NOT NULL;
ALTER TABLE pmi ADD CONSTRAINT uq_pmi_evaluation_area UNIQUE (self_evaluation_id, area_id);
