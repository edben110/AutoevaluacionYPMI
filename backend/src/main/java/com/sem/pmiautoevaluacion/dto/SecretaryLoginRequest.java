package com.sem.pmiautoevaluacion.dto;

import jakarta.validation.constraints.NotBlank;

// Cuerpo de la peticion para usuarios tipo Secretary
// requiere email + password
public class SecretaryLoginRequest {
    @NotBlank (message = "El correo es obligatorio")
    private String email;

    @NotBlank (message = "La contraseña es obligatoria")
    private String password;

    public SecretaryLoginRequest() {
        // Constructor vacio requerido para deserializacion JSON (Jackson)
    }

    public SecretaryLoginRequest(String email, String password){
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
