package com.sem.pmiautoevaluacion.pmi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FormulationRowRequest(
        UUID id,
        @Size(max = 500) String indicatorName,
        @Size(max = 10000) String activities,
        @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal totalCost,
        @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal fseAmount,
        @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal otherAmount,
        @Size(max = 500) String responsible,
        LocalDate startDate,
        LocalDate finishDate,
        @Size(max = 10000) String collectionInstruments,
        @Size(max = 500) String collectionFrequency) {}
