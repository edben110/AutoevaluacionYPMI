-- Ninguna autoevaluación previa se marca como enviada de manera automática.
CREATE TABLE self_evaluation_area_submission (
    id UUID PRIMARY KEY,
    evaluation_id UUID NOT NULL REFERENCES self_evaluation (id),
    area_id UUID NOT NULL REFERENCES area (id),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_self_evaluation_area_submission UNIQUE (evaluation_id, area_id)
);
