package com.sem.pmiautoevaluacion.pmi.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pmi_goal")
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objective_id", nullable = false)
    private Objective objective;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int position;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Activity> rows = new ArrayList<>();

    protected Goal() {}
    public Goal(Objective objective) { this.objective = objective; }
    public UUID getId() { return id; }
    public Objective getObjective() { return objective; }
    public String getDescription() { return description; }
    public int getPosition() { return position; }
    public List<Activity> getRows() { return rows; }
    public void update(String description, int position) {
        this.description = description;
        this.position = position;
    }
}
