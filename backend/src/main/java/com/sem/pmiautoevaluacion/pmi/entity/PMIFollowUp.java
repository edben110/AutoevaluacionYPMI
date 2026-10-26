package com.sem.pmiautoevaluacion.pmi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pmi_follow_up", uniqueConstraints = @UniqueConstraint(name = "uq_pmi_follow_up_date_row",
        columnNames = {"pmi_id", "control_date", "source_row_id"}))
public class PMIFollowUp {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pmi_id", nullable = false)
    private PMI pmi;
    @Column(name = "control_date", nullable = false)
    private LocalDate controlDate;
    // Identidad estable, sin borrado en cascada: se conserva aunque se elimine la fila de Anexo 3.
    @Column(name = "source_row_id", nullable = false)
    private UUID sourceRowId;
    @Embedded
    private FollowUpSnapshot snapshot;
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", length = 10)
    private ExecutionStatus executionStatus;
    @Column(name = "execution_percentage", precision = 5, scale = 2)
    private BigDecimal executionPercentage;
    @Column(columnDefinition = "TEXT")
    private String observations;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PMIFollowUp() {}
    public PMIFollowUp(PMI pmi, LocalDate controlDate, UUID sourceRowId, FollowUpSnapshot snapshot, Instant now) {
        this.pmi = pmi; this.controlDate = controlDate; this.sourceRowId = sourceRowId;
        this.snapshot = snapshot; this.createdAt = now; this.updatedAt = now;
    }
    public void update(ExecutionStatus status, BigDecimal percentage, String observations, Instant now) {
        this.executionStatus = status; this.executionPercentage = percentage;
        this.observations = observations; this.updatedAt = now;
    }
    public UUID getId() { return id; }
    public LocalDate getControlDate() { return controlDate; }
    public UUID getSourceRowId() { return sourceRowId; }
    public FollowUpSnapshot getSnapshot() { return snapshot; }
    public ExecutionStatus getExecutionStatus() { return executionStatus; }
    public BigDecimal getExecutionPercentage() { return executionPercentage; }
    public String getObservations() { return observations; }
    public Instant getUpdatedAt() { return updatedAt; }
}
