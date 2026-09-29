package com.sem.pmiautoevaluacion.auth.dto;

import java.util.UUID;

// Deliberadamente no incluye email ni codigo DANE
//  los puede conocer el frontend porque el usuario los escribe para autenticarse
public class LoginResponse {
    private String token;
    private UUID id;
    private String name;
    private String role; // "SECRETARY" o "ESTABLISHMENT"

    public LoginResponse() {
        // Constructor vacio requerido para serializacion JSON (Jackson)
    }

    public LoginResponse(
        String token,
        UUID id,
        String name,
        String role
    ){
        this.token = token;
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    
}
