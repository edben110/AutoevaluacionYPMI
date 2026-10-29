package com.sem.pmiautoevaluacion.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Pattern(regexp = "(?s).*\\S.*", message = "El nombre no puede estar vacio")
    String name,

    @Email(message = "El correo no tiene un formato valido")
    @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
    @Pattern(regexp = "(?s).*\\S.*", message = "El correo no puede estar vacio")
    String email,

    @Size(max = 255, message = "La contraseña no puede superar los 255 caracteres")
    @Pattern(regexp = "(?s).*\\S.*", message = "La contraseña no puede estar vacia")
    String password,

    @Size(max = 20, message = "El codigo DANE no puede superar los 20 caracteres")
    @Pattern(regexp = "(?s).*\\S.*", message = "El codigo DANE no puede estar vacio")
    String daneCode,

    @Size(max = 150, message = "El rector no puede superar los 150 caracteres")
    @Pattern(regexp = "(?s).*\\S.*", message = "El rector no puede estar vacio")
    String rector
) {}
