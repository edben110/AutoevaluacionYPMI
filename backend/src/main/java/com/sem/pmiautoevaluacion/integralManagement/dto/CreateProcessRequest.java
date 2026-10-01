package com.sem.pmiautoevaluacion.integralManagement.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// DTO de entrada para creacion
// TODO: Se necesitan DTO's de entrada para cada operacion, cuando coloquemos una operacion update hace falta crear
//      UpdateProcessRequest
// TODO: Hace falta crear los DTO's de las demas entidades -> Component, Area
public record CreateProcessRequest(
    @NotNull (message = "El id de area es obligatorio")
    UUID areaId,

    @NotBlank (message = "El nombre no puede estar vacio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    String name,

    String description
) {

}
