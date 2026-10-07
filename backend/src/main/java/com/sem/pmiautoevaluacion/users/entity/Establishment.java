package com.sem.pmiautoevaluacion.users.entity;

import java.util.ArrayList;
import java.util.List;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;

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

    @OneToMany(mappedBy = "establishment", fetch = FetchType.LAZY)
    private List<SelfEvaluation> selfEvaluations = new ArrayList<>();

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

    public List<SelfEvaluation> getSelfEvaluations() {
        return selfEvaluations;
    }

    public void setSelfEvaluations(List<SelfEvaluation> selfEvaluations) {
        this.selfEvaluations = selfEvaluations;
    }
}
