package com.sem.pmiautoevaluacion.pmi.dto;

import com.sem.pmiautoevaluacion.pmi.entity.ExecutionStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FollowUpRequest(
        ExecutionStatus executionStatus,
        @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal executionPercentage,
        @Size(max = 10000) String observations) {}
