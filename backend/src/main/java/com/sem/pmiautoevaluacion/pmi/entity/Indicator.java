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

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", length = 20)
    private IndicatorType type;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "measure_unit", length = 500)
    private String measureUnit;
    @Column(name = "variables_definition", columnDefinition = "TEXT")
    private String variablesDefinition;
    @Column(name = "data_formula", columnDefinition = "TEXT")
    private String dataFormula;
    @Column(name = "data_source", columnDefinition = "TEXT")
    private String dataSource;
    @Column(name = "methodological_aspects", columnDefinition = "TEXT")
    private String methodologicalAspects;
    @Column(name = "calculation_frequency", length = 500)
    private String calculationFrequency;
    @Column(name = "measurement_responsible", length = 500)
    private String measurementResponsible;

    protected Indicator() {}
    public Indicator(String name) { this.name = name; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void rename(String name) { this.name = name; }

    public IndicatorType getType() { return type; }
    public String getDescription() { return description; }
    public String getMeasureUnit() { return measureUnit; }
    public String getVariablesDefinition() { return variablesDefinition; }
    public String getDataFormula() { return dataFormula; }
    public String getDataSource() { return dataSource; }
    public String getMethodologicalAspects() { return methodologicalAspects; }
    public String getCalculationFrequency() { return calculationFrequency; }
    public String getMeasurementResponsible() { return measurementResponsible; }

    public void define(IndicatorType type, String description, String measureUnit, String variablesDefinition,
            String dataFormula, String dataSource, String methodologicalAspects, String calculationFrequency,
            String measurementResponsible) {
        this.type = type;
        this.description = description;
        this.measureUnit = measureUnit;
        this.variablesDefinition = variablesDefinition;
        this.dataFormula = dataFormula;
        this.dataSource = dataSource;
        this.methodologicalAspects = methodologicalAspects;
        this.calculationFrequency = calculationFrequency;
        this.measurementResponsible = measurementResponsible;
    }

    public boolean hasDefinition() {
        return type != null || java.util.stream.Stream.of(description, measureUnit, variablesDefinition,
                dataFormula, dataSource, methodologicalAspects, calculationFrequency, measurementResponsible)
                .anyMatch(value -> value != null && !value.isBlank());
    }

    public boolean isDefinitionComplete() {
        return name != null && !name.isBlank() && type != null && java.util.stream.Stream.of(description,
                measureUnit, variablesDefinition, dataFormula, dataSource, methodologicalAspects,
                calculationFrequency, measurementResponsible).allMatch(value -> value != null && !value.isBlank());
    }
}
