package com.sem.pmiautoevaluacion.pmi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record PMISelectionRequest(@NotNull @Size(min = 1, max = 2) List<@NotNull UUID> componentIds) {}
