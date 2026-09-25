package com.sem.pmiautoevaluacion.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity 
@Table(name = "establishment")
@DiscriminatorValue ("ESTABLISHMENT")
@PrimaryKeyJoinColumn (name = "id_establishment")
public class Establishment extends User{
    @NotBlank (message = "El codigo DANE no puede estar vacio")
    @Column (name = "dane_code", nullable = false, unique = true, length = 20)
    private String daneCode;

    @NotBlank (message = "El rector no puede estar vacio")
    @Column(name = "rector", nullable = false, length = 150)
    private String rector;

    protected Establishment() {
        super();
    }

    public Establishment(
        String name,
        String password,
        EmailRecord email,
        String daneCode,
        String rector
    ) {
        super(name, password, email);
        this.daneCode = daneCode;
        this.rector =  rector;
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

    // TODO: Agregar relacion @OneToMany cuando se defina la entidad relacionada a autoevaluacion

}
