package com.sem.pmiautoevaluacion.pmi.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PriorityComponentRequest(
        @NotBlank @Size(max = 10000) String criticalFactors,
        @NotNull @DecimalMin("1") @DecimalMax("5") @Digits(integer = 1, fraction = 0) BigDecimal urgency,
        @NotNull @DecimalMin("1") @DecimalMax("5") @Digits(integer = 1, fraction = 0) BigDecimal tendency,
        @NotNull @DecimalMin("1") @DecimalMax("5") @Digits(integer = 1, fraction = 0) BigDecimal impact) {}
