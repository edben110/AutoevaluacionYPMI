package com.sem.pmiautoevaluacion.review.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(UUID establishmentId, String establishmentName, String daneCode,
        int year, UUID areaId, String areaName, Instant submittedAt, UUID pmiId,
        Instant pmiUpdatedAt) {}
