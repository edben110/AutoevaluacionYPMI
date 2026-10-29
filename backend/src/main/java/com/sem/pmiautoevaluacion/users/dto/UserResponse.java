package com.sem.pmiautoevaluacion.users.dto;

import java.util.UUID;

import com.sem.pmiautoevaluacion.users.entity.User;

public class UserResponse {
    private UUID id;
    private String name;
    private String email;

    public UserResponse() {
        // Constructor vacio requerido para serializacion JSON
    }

    public static UserResponse fromEntity(User user){
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail().getValue());
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
}
