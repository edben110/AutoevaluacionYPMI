package com.sem.pmiautoevaluacion.pmi.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pmi_objective")
public class Objective {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prioritary_component_id", nullable = false)
    private PrioritaryComponent prioritaryComponent;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "objective", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Goal> goals = new ArrayList<>();

    protected Objective() {}
    public Objective(PrioritaryComponent prioritaryComponent) { this.prioritaryComponent = prioritaryComponent; }
    public UUID getId() { return id; }
    public PrioritaryComponent getPrioritaryComponent() { return prioritaryComponent; }
    public String getDescription() { return description; }
    public List<Goal> getGoals() { return goals; }
    public void update(PrioritaryComponent component, String description) {
        this.prioritaryComponent = component;
        this.description = description;
    }
}
