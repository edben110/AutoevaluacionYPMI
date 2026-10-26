package com.sem.pmiautoevaluacion.pmi.dto;

import com.sem.pmiautoevaluacion.pmi.entity.IndicatorType;

public record IndicatorDefinitionResponse(
        IndicatorType type, String description, String measureUnit, String variablesDefinition,
        String dataFormula, String dataSource, String methodologicalAspects,
        String calculationFrequency, String measurementResponsible) {}
