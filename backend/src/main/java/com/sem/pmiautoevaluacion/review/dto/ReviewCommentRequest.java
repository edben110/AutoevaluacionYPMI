package com.sem.pmiautoevaluacion.review.dto;

import jakarta.validation.constraints.*;

public record ReviewCommentRequest(@NotBlank @Size(max = 10000) String body) {}
