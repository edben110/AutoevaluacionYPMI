package com.sem.pmiautoevaluacion.users.dto;

import java.util.UUID;

import com.sem.pmiautoevaluacion.users.entity.Establishment;

public class EstablishmentResponse {
    private UUID id;
    private String daneCode;
    private String rector;

    // TODO: En caso de que los emails se remuevan para establecimiento, quitar
    private String email;
    private String name;

    public EstablishmentResponse() {
        // Constructor vacio requerido para serializacion JSON
    }

    public EstablishmentResponse fromEntity(Establishment establishment) {
        EstablishmentResponse response = new EstablishmentResponse();
        response.setId(establishment.getId());
        response.setDaneCode(establishment.getDaneCode());
        response.setRector(establishment.getRector());

        // TODO: En caso de que los emails se remuevan para establecimiento, quitar
        response.setName(establishment.getName());
        response.setEmail(establishment.getEmail().getValue());

        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    
}
