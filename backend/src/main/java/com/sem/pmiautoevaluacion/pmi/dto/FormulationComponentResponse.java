package com.sem.pmiautoevaluacion.pmi.dto;

import java.util.UUID;

public record FormulationComponentResponse(UUID componentId, String componentName,
        UUID areaId, String areaName, UUID processId, String processName, boolean eligible) {}
