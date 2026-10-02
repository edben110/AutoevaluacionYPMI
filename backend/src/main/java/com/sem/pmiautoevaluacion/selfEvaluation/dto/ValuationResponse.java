package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.ComponentValuation;
import java.time.Instant;
import java.util.UUID;

public record ValuationResponse(
        UUID componentId,
        short level,
        String evidenceUrl,
        String evidenceNote,
        Instant updatedAt
) {
    public static ValuationResponse from(ComponentValuation valuation) {
        return new ValuationResponse(
                valuation.getComponent().getId(), valuation.getLevel(),
                valuation.getEvidenceUrl(), valuation.getEvidenceNote(), valuation.getUpdatedAt());
    }
}
