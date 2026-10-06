package com.sem.pmiautoevaluacion.integralManagement.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// DTO de entrada para creacion
public record CreateComponentRequest (
    @NotNull (message = "El Id de proceso es obligatorio")
    UUID processId,

    @NotBlank (message = "El nombre no puede estar vacio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    String name,

    String description,

    String expectedEvidence
) {

}
