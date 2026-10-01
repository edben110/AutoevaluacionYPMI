package com.sem.pmiautoevaluacion.integralManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// TODO: Se necesitan DTO's de entrada para cada operacion, cuando coloquemos una operacion update hace falta crear
//      UpdateAreaRequest
public record CreateAreaRequest(
    @NotBlank (message = "El nombre no puede estar vacio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    String name,

    String description
) {

}
