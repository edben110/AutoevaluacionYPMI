package com.sem.pmiautoevaluacion.pmi.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PMIResponse(
        UUID id, UUID selfEvaluationId, int year, String status, int projectionYears,
        LocalDate filledOn, Instant updatedAt, String establishmentName,
        List<PriorityComponentResponse> components, List<UUID> highestComponentIds,
        List<UUID> selectedComponentIds, boolean selectionNeedsReview,
        UUID areaId, String areaName, boolean selfEvaluationComplete,
        int totalComponents, int valuedComponents, Integer candidateLevel, String candidateStateLabel,
        boolean prioritizationComplete, boolean canFormulate,
        boolean selfEvaluationSubmitted, Instant submittedAt, boolean periodConfigured) {}
