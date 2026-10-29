package com.sem.pmiautoevaluacion.selfEvaluation.entity;

import com.sem.pmiautoevaluacion.integralManagement.entity.Area;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "self_evaluation_area_submission", uniqueConstraints = @UniqueConstraint(
        name = "uq_self_evaluation_area_submission", columnNames = {"evaluation_id", "area_id"}))
public class SelfEvaluationAreaSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private SelfEvaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected SelfEvaluationAreaSubmission() {}

    public SelfEvaluationAreaSubmission(SelfEvaluation evaluation, Area area, Instant submittedAt) {
        this.evaluation = evaluation;
        this.area = area;
        this.submittedAt = submittedAt;
    }

    public UUID getId() { return id; }
    public SelfEvaluation getEvaluation() { return evaluation; }
    public Area getArea() { return area; }
    public Instant getSubmittedAt() { return submittedAt; }
}
