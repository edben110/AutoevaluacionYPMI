-- Las observaciones de SEM nunca modifican las respuestas institucionales.
CREATE TABLE component_review_comment (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES self_evaluation_area_submission(id) ON DELETE CASCADE,
    valuation_id UUID NOT NULL REFERENCES component_valuation(id) ON DELETE CASCADE,
    secretary_id UUID NOT NULL REFERENCES "user"(id_user),
    author_name VARCHAR(150) NOT NULL,
    body TEXT NOT NULL CHECK (length(trim(body)) > 0 AND length(body) <= 10000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_component_review_submission ON component_review_comment(submission_id, created_at, id);
