package com.sem.pmiautoevaluacion.pmi.dto;

import com.sem.pmiautoevaluacion.pmi.entity.IndicatorType;
import jakarta.validation.constraints.Size;

public record IndicatorDefinitionRequest(
        IndicatorType type,
        @Size(max = 10000) String description,
        @Size(max = 500) String measureUnit,
        @Size(max = 10000) String variablesDefinition,
        @Size(max = 10000) String dataFormula,
        @Size(max = 10000) String dataSource,
        @Size(max = 10000) String methodologicalAspects,
        @Size(max = 500) String calculationFrequency,
        @Size(max = 500) String measurementResponsible) {}
