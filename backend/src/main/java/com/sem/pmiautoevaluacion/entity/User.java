package com.sem.pmiautoevaluacion.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Entity 
@Table(name = "\"user\"")
@Inheritance (strategy = InheritanceType.JOINED)
@DiscriminatorColumn (name = "role", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user")
    private UUID id;

    @NotBlank(message = "El nombre no puede estar vacio")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank(message = "La contraseña no puede estar vacia")
    @Column(name = "password", nullable = false, length=255)
    private String password;

    @Embedded
    private EmailRecord email;

    protected User(){
        // Constructor vacio requerido por JPA
    }

    protected User(String name, String password, EmailRecord email){
        this.name = name;
        this.password = password;
        this.email = email;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public EmailRecord getEmail() {
        return email;
    }

    public void setEmail(EmailRecord email) {
        this.email = email;
    }    
}
