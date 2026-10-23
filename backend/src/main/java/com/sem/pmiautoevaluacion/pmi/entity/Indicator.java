package com.sem.pmiautoevaluacion.pmi.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "pmi_indicator")
public class Indicator {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(length = 500)
    private String name;

    protected Indicator() {}
    public Indicator(String name) { this.name = name; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void rename(String name) { this.name = name; }
}
