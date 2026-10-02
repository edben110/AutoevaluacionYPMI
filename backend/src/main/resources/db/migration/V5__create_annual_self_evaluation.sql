CREATE TABLE self_evaluation (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL REFERENCES establishment (id_establishment),
    year INTEGER NOT NULL CHECK (year > 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_self_evaluation_establishment_year UNIQUE (establishment_id, year)
);

CREATE TABLE component_valuation (
    id UUID PRIMARY KEY,
    evaluation_id UUID NOT NULL REFERENCES self_evaluation (id) ON DELETE CASCADE,
    component_id UUID NOT NULL REFERENCES component (id),
    level SMALLINT NOT NULL CHECK (level BETWEEN 1 AND 4),
    evidence_url VARCHAR(1000),
    evidence_note TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_component_valuation_evaluation_component UNIQUE (evaluation_id, component_id)
);
