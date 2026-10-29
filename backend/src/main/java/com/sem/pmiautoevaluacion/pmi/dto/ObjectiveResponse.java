package com.sem.pmiautoevaluacion.pmi.dto;

import java.util.List;
import java.util.UUID;

public record ObjectiveResponse(
        UUID id, UUID componentId, String componentName, UUID areaId, String areaName,
        UUID processId, String processName, String description, boolean editable,
        List<FormulationGoalResponse> goals) {}
