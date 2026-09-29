package com.sem.pmiautoevaluacion.users.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sem.pmiautoevaluacion.entity.Establishment;
import com.sem.pmiautoevaluacion.entity.Secretary;
import com.sem.pmiautoevaluacion.entity.User;

/**
 * Datos del usuario actualmente autenticado (endpoing GET: /api/me)
 * DaneCode y Rector solo aplican a instituciones, en secretarias
 * llegan como null y se omiten del JSON gracias a Non_null
 * MeResponse
 */
@JsonInclude (JsonInclude.Include.NON_NULL)
public class MeResponse {
    private UUID id;
    private String name;
    private String email;
    private String role;
    private String daneCode;
    private String rector;

    public MeResponse() {
        // Constructor vacio requerido para serializacion JSON
    }

    public static MeResponse fromEntity(User user) {
        MeResponse response = new MeResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail().getValue());
        
        if (user instanceof Establishment establishment) {
            response.setRole("ESTABLISHMENT");
            response.setDaneCode(establishment.getDaneCode());
            response.setRector(establishment.getRector());
        } else if (user instanceof Secretary) {
            response.setRole("SECRETARY");
        }

        return response;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDaneCode() {
        return daneCode;
    }

    public void setDaneCode(String daneCode) {
        this.daneCode = daneCode;
    }

    public String getRector() {
        return rector;
    }

    public void setRector(String rector) {
        this.rector = rector;
    }

    
}
