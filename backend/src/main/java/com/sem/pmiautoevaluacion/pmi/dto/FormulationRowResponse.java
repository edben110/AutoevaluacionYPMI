package com.sem.pmiautoevaluacion.pmi.dto;

import java.time.LocalDate;
import java.util.UUID;

public record FormulationRowResponse(
        UUID id, String indicatorName, String activities, String totalCost,
        String fseAmount, String otherAmount, String responsible,
        LocalDate startDate, LocalDate finishDate, String collectionInstruments,
        String collectionFrequency) {}
