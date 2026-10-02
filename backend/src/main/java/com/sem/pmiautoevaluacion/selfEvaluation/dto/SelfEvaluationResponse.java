package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SelfEvaluationResponse(
        UUID id,
        int year,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<ValuationResponse> valuations
) {
    public static SelfEvaluationResponse from(SelfEvaluation evaluation, List<ValuationResponse> valuations) {
        return new SelfEvaluationResponse(
                evaluation.getId(), evaluation.getYear(), evaluation.getStatus().name(),
                evaluation.getCreatedAt(), evaluation.getUpdatedAt(), valuations);
    }
}
