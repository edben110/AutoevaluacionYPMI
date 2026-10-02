CREATE TABLE area (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    state VARCHAR(255)
);

CREATE TABLE process (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    state VARCHAR(255),
    area_id UUID NOT NULL,

    CONSTRAINT fk_process_area
        FOREIGN KEY (area_id) REFERENCES area (id)
);

CREATE TABLE component (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    state VARCHAR(255),
    expected_evidence TEXT,
    process_id UUID NOT NULL,

    CONSTRAINT fk_component_process
        FOREIGN KEY (process_id) REFERENCES process (id)
);
