package com.sem.pmiautoevaluacion.integralManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAreaRequest(
    @NotBlank (message = "El nombre no puede estar vacio")
    @Size (max = 255)
    String name,
    String description
) {

}
