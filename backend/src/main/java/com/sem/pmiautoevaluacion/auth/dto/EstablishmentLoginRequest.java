package com.sem.pmiautoevaluacion.auth.dto;

import jakarta.validation.constraints.NotBlank;

// Cuerpo de la peticion para usuarios de tipo Establishment
// Se requiere daneCode + password
public class EstablishmentLoginRequest {
    @NotBlank (message = "El codigo DANE es obligatorio")
    private String daneCode;

    @NotBlank (message = "La contraseña es obligatoria")
    private String password;

    public EstablishmentLoginRequest() {
        // Constructor vacio para deserializacion JSON (Jackson)
    }

    public EstablishmentLoginRequest(String daneCode, String password){
        this.daneCode = daneCode;
        this.password = password;
    }

    public String getDaneCode() {
        return daneCode;
    }

    public void setDaneCode(String daneCode) {
        this.daneCode = daneCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
