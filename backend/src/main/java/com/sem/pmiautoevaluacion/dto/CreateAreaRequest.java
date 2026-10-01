package com.sem.pmiautoevaluacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAreaRequest(
    @NotBlank (message = "El nombre no puede estar vacio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    String name,

    String description
) {

}
