CREATE TABLE pmi (
    id UUID PRIMARY KEY,
    self_evaluation_id UUID NOT NULL UNIQUE REFERENCES self_evaluation (id),
    projection_years INTEGER NOT NULL CHECK (projection_years = 1),
    filled_on DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE prioritary_component (
    id UUID PRIMARY KEY,
    pmi_id UUID NOT NULL REFERENCES pmi (id),
    valuation_id UUID NOT NULL REFERENCES component_valuation (id),
    critical_factors TEXT NOT NULL CHECK (length(trim(critical_factors)) BETWEEN 1 AND 10000),
    urgency INTEGER NOT NULL CHECK (urgency BETWEEN 1 AND 5),
    tendency INTEGER NOT NULL CHECK (tendency BETWEEN 1 AND 5),
    impact INTEGER NOT NULL CHECK (impact BETWEEN 1 AND 5),
    selected_slot INTEGER CHECK (selected_slot IN (1, 2)),
    CONSTRAINT uq_priority_pmi_valuation UNIQUE (pmi_id, valuation_id),
    CONSTRAINT uq_priority_pmi_slot UNIQUE (pmi_id, selected_slot) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX ix_prioritary_component_valuation ON prioritary_component (valuation_id);
