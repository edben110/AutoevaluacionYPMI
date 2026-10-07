package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SelfEvaluationPeriodRequest(
        @NotNull(message = "Indica la fecha de inicio") LocalDate startDate,
        @NotNull(message = "Indica la fecha de fin") LocalDate finishDate) {}
