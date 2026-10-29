package com.sem.pmiautoevaluacion.pmi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/** Contexto del plan cuando se guarda por primera vez una fila en una fecha de control. */
@Embeddable
public class FollowUpSnapshot {
    @Column(name = "source_component_id", nullable = false)
    private UUID componentId;
    @Column(name = "source_objective_id", nullable = false)
    private UUID objectiveId;
    @Column(name = "source_goal_id", nullable = false)
    private UUID goalId;
    @Column(name = "source_process_name", nullable = false, columnDefinition = "TEXT")
    private String processName;
    @Column(name = "source_component_name", nullable = false, columnDefinition = "TEXT")
    private String componentName;
    @Column(name = "source_objective_description", columnDefinition = "TEXT")
    private String objectiveDescription;
    @Column(name = "source_goal_description", columnDefinition = "TEXT")
    private String goalDescription;
    @Column(name = "source_indicator_name", length = 500)
    private String indicatorName;
    @Column(name = "source_activities", columnDefinition = "TEXT")
    private String activities;
    @Column(name = "source_start_date")
    private LocalDate startDate;
    @Column(name = "source_finish_date")
    private LocalDate finishDate;

    protected FollowUpSnapshot() {}
    public FollowUpSnapshot(UUID componentId, UUID objectiveId, UUID goalId, String processName,
            String componentName, String objectiveDescription, String goalDescription, String indicatorName,
            String activities, LocalDate startDate, LocalDate finishDate) {
        this.componentId = componentId; this.objectiveId = objectiveId; this.goalId = goalId;
        this.processName = processName; this.componentName = componentName;
        this.objectiveDescription = objectiveDescription; this.goalDescription = goalDescription;
        this.indicatorName = indicatorName; this.activities = activities;
        this.startDate = startDate; this.finishDate = finishDate;
    }
    public UUID getComponentId() { return componentId; }
    public UUID getObjectiveId() { return objectiveId; }
    public UUID getGoalId() { return goalId; }
    public String getProcessName() { return processName; }
    public String getComponentName() { return componentName; }
    public String getObjectiveDescription() { return objectiveDescription; }
    public String getGoalDescription() { return goalDescription; }
    public String getIndicatorName() { return indicatorName; }
    public String getActivities() { return activities; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getFinishDate() { return finishDate; }
}
