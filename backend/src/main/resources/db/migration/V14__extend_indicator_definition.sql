-- Anexo 4: ampliar el indicador existente sin duplicar su nombre ni su vínculo con Anexo 3.
ALTER TABLE pmi_indicator
    ADD COLUMN indicator_type VARCHAR(20) CHECK (indicator_type IN ('PROCESS', 'RESULT')),
    ADD COLUMN description TEXT CHECK (description IS NULL OR length(description) <= 10000),
    ADD COLUMN measure_unit VARCHAR(500),
    ADD COLUMN variables_definition TEXT CHECK (variables_definition IS NULL OR length(variables_definition) <= 10000),
    ADD COLUMN data_formula TEXT CHECK (data_formula IS NULL OR length(data_formula) <= 10000),
    ADD COLUMN data_source TEXT CHECK (data_source IS NULL OR length(data_source) <= 10000),
    ADD COLUMN methodological_aspects TEXT CHECK (methodological_aspects IS NULL OR length(methodological_aspects) <= 10000),
    ADD COLUMN calculation_frequency VARCHAR(500),
    ADD COLUMN measurement_responsible VARCHAR(500);
