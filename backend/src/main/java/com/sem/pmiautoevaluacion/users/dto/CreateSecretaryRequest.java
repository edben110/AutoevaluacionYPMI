package com.sem.pmiautoevaluacion.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateSecretaryRequest(
    @NotBlank(message = "El nombre es obligatorio")
    String name,

    @NotBlank(message = "La contraseña es obligatoria")
    String password,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    String email
) {}
