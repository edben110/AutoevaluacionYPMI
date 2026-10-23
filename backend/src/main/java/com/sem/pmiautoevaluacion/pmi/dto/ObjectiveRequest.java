package com.sem.pmiautoevaluacion.pmi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ObjectiveRequest(
        @NotNull UUID componentId,
        @Size(max = 10000) String description,
        @NotNull @Valid List<@NotNull FormulationGoalRequest> goals) {}
