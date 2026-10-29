package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import java.time.Instant;
import java.util.UUID;

public record SelfEvaluationSummary(UUID id, int year, String status, Instant updatedAt) {
    public static SelfEvaluationSummary from(SelfEvaluation evaluation) {
        return new SelfEvaluationSummary(evaluation.getId(), evaluation.getYear(),
                evaluation.getStatus().name(), evaluation.getUpdatedAt());
    }
}
