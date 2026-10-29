package com.sem.pmiautoevaluacion.pmi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record FormulationGoalRequest(
        UUID id,
        @Size(max = 10000) String description,
        @NotNull @Valid List<@NotNull FormulationRowRequest> rows) {}
