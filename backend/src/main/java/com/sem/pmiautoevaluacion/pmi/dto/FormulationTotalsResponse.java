package com.sem.pmiautoevaluacion.pmi.dto;

public record FormulationTotalsResponse(String totalCost, String fseAmount,
        String otherAmount, int pendingAmountFields, int excludedObjectives) {}
