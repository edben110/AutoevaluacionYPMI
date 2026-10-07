CREATE TABLE self_evaluation_period (
    year INTEGER PRIMARY KEY CHECK (year BETWEEN 1 AND 9999),
    start_date DATE NOT NULL,
    finish_date DATE NOT NULL,
    created_by UUID NOT NULL REFERENCES secretary (id_user),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_self_evaluation_period_dates CHECK (finish_date >= start_date)
);

-- Los borradores anteriores se conservan; Secretaría vincula el período al configurarlo.
ALTER TABLE self_evaluation ADD COLUMN period_year INTEGER
    REFERENCES self_evaluation_period (year);
ALTER TABLE self_evaluation ADD CONSTRAINT ck_self_evaluation_period_year
    CHECK (period_year IS NULL OR period_year = year);
