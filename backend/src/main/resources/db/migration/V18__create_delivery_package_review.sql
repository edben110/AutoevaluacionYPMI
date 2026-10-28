CREATE TABLE delivery_package_review (
    id UUID PRIMARY KEY,
    evaluation_id UUID NOT NULL UNIQUE REFERENCES self_evaluation(id) ON DELETE CASCADE,
    secretary_id UUID NOT NULL REFERENCES "user"(id_user),
    author_name VARCHAR(150) NOT NULL,
    source_version VARCHAR(64) NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
