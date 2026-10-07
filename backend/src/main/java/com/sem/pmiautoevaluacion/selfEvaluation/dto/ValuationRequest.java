package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValuationRequest(
        @NotNull @Min(1) @Max(4) Integer level,
        @Size(max = 1000) String evidenceUrl,
        @Size(max = 10000) String evidenceNote,
        @Size(max = 10000) String strengths,
        @Size(max = 10000) String improvementOpportunities
) {}
