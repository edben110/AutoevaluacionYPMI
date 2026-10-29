package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import java.time.Instant;
import java.util.UUID;

public record SelfEvaluationAreaResponse(int year, UUID areaId, String areaName, String status,
        Instant submittedAt, int totalComponents, int valuedComponents, boolean complete, boolean canSubmit) {}
