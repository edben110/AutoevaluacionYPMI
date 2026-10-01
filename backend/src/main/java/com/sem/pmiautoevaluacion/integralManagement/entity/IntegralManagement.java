package com.sem.pmiautoevaluacion.integralManagement.entity;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import java.util.UUID;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@MappedSuperclass 
public abstract class IntegralManagement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "El nombre no puede estar vacio")
    @Column(name = "name",nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated (EnumType.STRING)
    private UseState state;

    protected IntegralManagement(){
        // Constructor vacio para JPA
    }

    protected IntegralManagement(String name, String description){
        this.name = name;
        this.description = description;
        this.state = UseState.ACTIVE;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public UseState getState() {
        return state;
    }

    public void setState(UseState state) {
        this.state = state;
    }
    
}
