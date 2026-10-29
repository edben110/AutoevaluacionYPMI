package com.sem.pmiautoevaluacion.pmi.dto;

import java.util.List;
import java.util.UUID;

public record FormulationGoalResponse(UUID id, String description, List<FormulationRowResponse> rows) {}
