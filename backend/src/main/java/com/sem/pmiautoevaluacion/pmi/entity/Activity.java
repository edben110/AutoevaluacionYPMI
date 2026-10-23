package com.sem.pmiautoevaluacion.pmi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Una fila del Anexo 3: su descripción puede contener un bloque de varias acciones. */
@Entity
@Table(name = "pmi_activity")
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "indicator_id", nullable = false, unique = true)
    private Indicator indicator;

    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "total_cost", precision = 16, scale = 2)
    private BigDecimal totalCost;
    @Column(name = "fse_amount", precision = 16, scale = 2)
    private BigDecimal fseAmount;
    @Column(name = "other_amount", precision = 16, scale = 2)
    private BigDecimal otherAmount;
    @Column(length = 500)
    private String responsible;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "finish_date")
    private LocalDate finishDate;
    @Column(name = "collection_instruments", columnDefinition = "TEXT")
    private String collectionInstruments;
    @Column(name = "collection_frequency", length = 500)
    private String collectionFrequency;
    @Column(name = "display_order", nullable = false)
    private int position;

    protected Activity() {}
    public Activity(Goal goal) {
        this.goal = goal;
        this.indicator = new Indicator(null);
    }

    public UUID getId() { return id; }
    public Goal getGoal() { return goal; }
    public Indicator getIndicator() { return indicator; }
    public String getDescription() { return description; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getFseAmount() { return fseAmount; }
    public BigDecimal getOtherAmount() { return otherAmount; }
    public String getResponsible() { return responsible; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getFinishDate() { return finishDate; }
    public String getCollectionInstruments() { return collectionInstruments; }
    public String getCollectionFrequency() { return collectionFrequency; }
    public int getPosition() { return position; }

    public void update(String indicatorName, String description, BigDecimal totalCost,
            BigDecimal fseAmount, BigDecimal otherAmount, String responsible, LocalDate startDate,
            LocalDate finishDate, String collectionInstruments, String collectionFrequency, int position) {
        this.indicator.rename(indicatorName);
        this.description = description;
        this.totalCost = totalCost;
        this.fseAmount = fseAmount;
        this.otherAmount = otherAmount;
        this.responsible = responsible;
        this.startDate = startDate;
        this.finishDate = finishDate;
        this.collectionInstruments = collectionInstruments;
        this.collectionFrequency = collectionFrequency;
        this.position = position;
    }
}
