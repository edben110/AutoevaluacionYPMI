package com.sem.pmiautoevaluacion.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEstablishmentRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150)
    String name,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 255)
    String password,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    @Size(max = 150)
    String email,

    @NotBlank(message = "El codigo DANE es obligatorio")
    @Size(max = 20)
    String daneCode,

    @NotBlank(message = "El rector es obligatorio")
    @Size(max = 150)
    String rector
) {}
