package com.sem.pmiautoevaluacion.pmi.entity;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.integralManagement.entity.Area;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pmi", uniqueConstraints = @UniqueConstraint(name = "uq_pmi_evaluation_area", columnNames = {"self_evaluation_id", "area_id"}))
public class PMI {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "self_evaluation_id", nullable = false)
    private SelfEvaluation selfEvaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(name = "projection_years", nullable = false)
    private int projectionYears;

    @Column(name = "filled_on", nullable = false)
    private LocalDate filledOn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    protected PMI() {}

    public PMI(SelfEvaluation selfEvaluation, Area area, LocalDate filledOn, Instant now) {
        this.selfEvaluation = selfEvaluation;
        this.area = area;
        this.projectionYears = 1;
        this.filledOn = filledOn;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public SelfEvaluation getSelfEvaluation() { return selfEvaluation; }
    public Area getArea() { return area; }
    public int getProjectionYears() { return projectionYears; }
    public LocalDate getFilledOn() { return filledOn; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public boolean isSubmitted() { return submittedAt != null; }
    public void submit(Instant now) {
        if (submittedAt == null) { submittedAt = now; updatedAt = now; }
    }
    public void touch(Instant now) { updatedAt = now; }
}
