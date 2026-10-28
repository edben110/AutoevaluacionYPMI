package com.sem.pmiautoevaluacion.review.dto;

import jakarta.validation.constraints.*;

public record PackageReviewRequest(@NotBlank @Pattern(regexp = "[a-f0-9]{64}") String version) {}
