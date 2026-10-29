CREATE TABLE pmi_follow_up (
    id UUID PRIMARY KEY,
    pmi_id UUID NOT NULL REFERENCES pmi (id),
    control_date DATE NOT NULL,
    source_row_id UUID NOT NULL,
    source_component_id UUID NOT NULL,
    source_objective_id UUID NOT NULL,
    source_goal_id UUID NOT NULL,
    source_process_name TEXT NOT NULL,
    source_component_name TEXT NOT NULL,
    source_objective_description TEXT,
    source_goal_description TEXT,
    source_indicator_name VARCHAR(500),
    source_activities TEXT,
    source_start_date DATE,
    source_finish_date DATE,
    execution_status VARCHAR(10) CHECK (execution_status IN ('NI', 'ESP', 'CANC', 'FIN', 'EJ')),
    execution_percentage NUMERIC(5,2) CHECK (execution_percentage BETWEEN 0 AND 100),
    observations TEXT CHECK (observations IS NULL OR length(observations) <= 10000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_pmi_follow_up_date_row UNIQUE (pmi_id, control_date, source_row_id)
);
-- El contexto de origen es una instantánea: eliminar o editar el plan no reescribe controles guardados.
CREATE INDEX ix_pmi_follow_up_plan_date ON pmi_follow_up (pmi_id, control_date);
