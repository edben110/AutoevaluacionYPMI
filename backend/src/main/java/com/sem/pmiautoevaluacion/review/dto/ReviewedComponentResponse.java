package com.sem.pmiautoevaluacion.review.dto;

import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationResponse;
import java.util.UUID;

public record ReviewedComponentResponse(UUID componentId, String componentName, String description,
        UUID processId, String processName, ValuationResponse valuation) {}
