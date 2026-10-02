package com.sem.pmiautoevaluacion.selfEvaluation.entity;

import com.sem.pmiautoevaluacion.users.entity.Establishment;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "self_evaluation", uniqueConstraints =
        @UniqueConstraint(name = "uq_self_evaluation_establishment_year", columnNames = {"establishment_id", "year"}))
public class SelfEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    @Column(name = "year", nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SelfEvaluationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SelfEvaluation() {}

    public SelfEvaluation(Establishment establishment, int year) {
        this.establishment = establishment;
        this.year = year;
        this.status = SelfEvaluationStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public Establishment getEstablishment() { return establishment; }
    public int getYear() { return year; }
    public SelfEvaluationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { updatedAt = Instant.now(); }
}
