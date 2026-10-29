package com.sem.pmiautoevaluacion.integralManagement.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ChangeProcessRequest(
    @NotNull (message = "El id del proceso es obligatorio")
    UUID processId
) {

}
