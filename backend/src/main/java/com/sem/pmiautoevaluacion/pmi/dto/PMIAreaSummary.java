package com.sem.pmiautoevaluacion.pmi.dto;

import java.util.UUID;
import java.time.Instant;

public record PMIAreaSummary(UUID areaId, String areaName, int totalComponents, int valuedComponents,
        boolean selfEvaluationComplete, Integer candidateLevel, String candidateStateLabel, int candidateCount,
        boolean selfEvaluationSubmitted, Instant submittedAt) {}
