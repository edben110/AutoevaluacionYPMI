package com.sem.pmiautoevaluacion.integralManagement.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// DTO de entrada para creacion
public record CreateProcessRequest(
    @NotNull (message = "El id de area es obligatorio")
    UUID areaId,

    @NotBlank (message = "El nombre no puede estar vacio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    String name,

    String description
) {

}
