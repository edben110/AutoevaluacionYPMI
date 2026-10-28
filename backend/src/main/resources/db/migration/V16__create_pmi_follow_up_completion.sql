-- La finalización es explícita; ningún control anterior se considera finalizado automáticamente.
CREATE TABLE pmi_follow_up_completion (
    id UUID PRIMARY KEY,
    pmi_id UUID NOT NULL UNIQUE REFERENCES pmi(id),
    review_year INTEGER NOT NULL CHECK (review_year BETWEEN 1 AND 9999),
    control_date DATE NOT NULL,
    source_fingerprint VARCHAR(64) NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
