package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.ComponentValuation;
import java.time.Instant;
import java.util.UUID;

public record ValuationResponse(
        UUID componentId,
        short level,
        String state,
        String stateLabel,
        boolean improvementChance,
        boolean strength,
        boolean strengthsAllowed,
        boolean improvementOpportunitiesAllowed,
        String evidenceUrl,
        String evidenceNote,
        String strengths,
        String improvementOpportunities,
        Instant updatedAt
) {
    public static ValuationResponse from(ComponentValuation valuation) {
        return new ValuationResponse(
                valuation.getComponent().getId(), valuation.getLevel(),
                valuation.getValuationState().code(), valuation.getValuationState().label(),
                valuation.isImprovementChance(), valuation.isStrength(),
                valuation.getValuationState().allowsStrengths(),
                valuation.getValuationState().allowsImprovementOpportunities(),
                valuation.getEvidenceUrl(), valuation.getEvidenceNote(),
                valuation.getStrengths(), valuation.getImprovementOpportunities(), valuation.getUpdatedAt());
    }
}
