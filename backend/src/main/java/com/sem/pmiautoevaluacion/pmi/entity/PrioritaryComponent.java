package com.sem.pmiautoevaluacion.pmi.entity;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.ComponentValuation;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "prioritary_component", uniqueConstraints = {
        @UniqueConstraint(name = "uq_priority_pmi_valuation", columnNames = {"pmi_id", "valuation_id"})})
public class PrioritaryComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pmi_id", nullable = false)
    private PMI pmi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "valuation_id", nullable = false)
    private ComponentValuation valuation;

    @Column(name = "critical_factors", columnDefinition = "TEXT", nullable = false)
    private String criticalFactors;

    @Column(nullable = false)
    private int urgency;
    @Column(nullable = false)
    private int tendency;
    @Column(nullable = false)
    private int impact;

    @Column(name = "selected_slot")
    private Integer selectedSlot;

    protected PrioritaryComponent() {}

    public PrioritaryComponent(PMI pmi, ComponentValuation valuation) {
        this.pmi = pmi;
        this.valuation = valuation;
    }

    public void update(String criticalFactors, int urgency, int tendency, int impact) {
        if (criticalFactors == null || criticalFactors.isBlank() || criticalFactors.trim().length() > 10000) {
            throw new BadRequestException("Describe los factores críticos en un máximo de 10000 caracteres");
        }
        if (urgency < 1 || urgency > 5 || tendency < 1 || tendency > 5 || impact < 1 || impact > 5) {
            throw new BadRequestException("Urgencia, tendencia e impacto deben estar entre 1 y 5");
        }
        this.criticalFactors = criticalFactors.trim();
        this.urgency = urgency;
        this.tendency = tendency;
        this.impact = impact;
    }

    public UUID getId() { return id; }
    public PMI getPmi() { return pmi; }
    public ComponentValuation getValuation() { return valuation; }
    public String getCriticalFactors() { return criticalFactors; }
    public int getUrgency() { return urgency; }
    public int getTendency() { return tendency; }
    public int getImpact() { return impact; }
    public int getSumUTI() { return urgency + tendency + impact; }
    public Integer getSelectedSlot() { return selectedSlot; }
    public void select(Integer slot) {
        if (slot != null && slot != 1 && slot != 2) {
            throw new IllegalArgumentException("La selección debe ocupar la posición 1 o 2");
        }
        selectedSlot = slot;
    }
}
