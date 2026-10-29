CREATE TABLE pmi_objective (
    id UUID PRIMARY KEY,
    prioritary_component_id UUID NOT NULL REFERENCES prioritary_component (id),
    description TEXT CHECK (description IS NULL OR length(description) <= 10000)
);
CREATE INDEX ix_pmi_objective_component ON pmi_objective (prioritary_component_id);

CREATE TABLE pmi_goal (
    id UUID PRIMARY KEY,
    objective_id UUID NOT NULL REFERENCES pmi_objective (id),
    description TEXT CHECK (description IS NULL OR length(description) <= 10000),
    display_order INTEGER NOT NULL CHECK (display_order >= 0)
);
CREATE INDEX ix_pmi_goal_objective ON pmi_goal (objective_id);

CREATE TABLE pmi_indicator (
    id UUID PRIMARY KEY,
    name VARCHAR(500)
);

CREATE TABLE pmi_activity (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES pmi_goal (id),
    indicator_id UUID NOT NULL UNIQUE REFERENCES pmi_indicator (id),
    description TEXT CHECK (description IS NULL OR length(description) <= 10000),
    total_cost NUMERIC(16,2) CHECK (total_cost >= 0),
    fse_amount NUMERIC(16,2) CHECK (fse_amount >= 0),
    other_amount NUMERIC(16,2) CHECK (other_amount >= 0),
    responsible VARCHAR(500),
    start_date DATE,
    finish_date DATE,
    collection_instruments TEXT CHECK (collection_instruments IS NULL OR length(collection_instruments) <= 10000),
    collection_frequency VARCHAR(500),
    display_order INTEGER NOT NULL CHECK (display_order >= 0),
    CONSTRAINT ck_pmi_activity_dates CHECK (start_date IS NULL OR finish_date IS NULL OR finish_date >= start_date)
);
CREATE INDEX ix_pmi_activity_goal ON pmi_activity (goal_id);
