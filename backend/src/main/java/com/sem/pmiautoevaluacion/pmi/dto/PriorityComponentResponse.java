package com.sem.pmiautoevaluacion.pmi.dto;

import java.util.UUID;

public record PriorityComponentResponse(
        UUID componentId, UUID valuationId, UUID areaId, String areaName,
        UUID processId, String processName, String componentName,
        short level, String state, String stateLabel, String improvementOpportunities,
        boolean eligible, String criticalFactors, Integer urgency, Integer tendency,
        Integer impact, Integer total, boolean selected) {}
