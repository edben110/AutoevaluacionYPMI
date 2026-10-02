package com.sem.pmiautoevaluacion.selfEvaluation.entity;

import com.sem.pmiautoevaluacion.integralManagement.entity.Component;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "component_valuation", uniqueConstraints =
        @UniqueConstraint(name = "uq_component_valuation_evaluation_component", columnNames = {"evaluation_id", "component_id"}))
public class ComponentValuation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private SelfEvaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private Component component;

    @Column(name = "level", nullable = false)
    private short level;

    @Column(name = "evidence_url", length = 1000)
    private String evidenceUrl;

    @Column(name = "evidence_note", columnDefinition = "TEXT")
    private String evidenceNote;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ComponentValuation() {}

    public ComponentValuation(SelfEvaluation evaluation, Component component) {
        this.evaluation = evaluation;
        this.component = component;
    }

    public void update(short level, String evidenceUrl, String evidenceNote) {
        this.level = level;
        this.evidenceUrl = evidenceUrl;
        this.evidenceNote = evidenceNote;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public SelfEvaluation getEvaluation() { return evaluation; }
    public Component getComponent() { return component; }
    public short getLevel() { return level; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public String getEvidenceNote() { return evidenceNote; }
    public Instant getUpdatedAt() { return updatedAt; }
}
