package com.sem.pmiautoevaluacion.pmi.dto;

import java.time.Instant;
import java.util.UUID;

public record PreviousFollowUpResponse(UUID pmiId, int year, UUID areaId, String areaName,
        boolean completed, Instant completedAt, int requiredRows) {}
