package com.sem.pmiautoevaluacion.integralManagement.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ChangeAreaRequest(
    @NotNull(message = "El id del area es obligatorio")
    UUID areaId
) {

}
