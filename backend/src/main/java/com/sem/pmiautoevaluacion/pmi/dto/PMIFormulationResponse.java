package com.sem.pmiautoevaluacion.pmi.dto;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PMIFormulationResponse(UUID pmiId, int year, String establishmentName,
        int projectionYears, LocalDate filledOn, boolean selectionNeedsReview,
        List<FormulationComponentResponse> selectedComponents, List<ObjectiveResponse> objectives,
        FormulationTotalsResponse totals, UUID areaId, String areaName,
        boolean selfEvaluationComplete, boolean prioritizationComplete, boolean canFormulate,
        boolean selfEvaluationSubmitted, Instant submittedAt) {}
