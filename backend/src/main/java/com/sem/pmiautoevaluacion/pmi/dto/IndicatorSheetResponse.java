package com.sem.pmiautoevaluacion.pmi.dto;

import java.util.UUID;

public record IndicatorSheetResponse(
        UUID id, UUID rowId, UUID objectiveId, UUID goalId, UUID componentId,
        String componentName, String processName, String objectiveDescription,
        String goalDescription, String name, boolean editable, boolean complete,
        IndicatorDefinitionResponse definition) {}
