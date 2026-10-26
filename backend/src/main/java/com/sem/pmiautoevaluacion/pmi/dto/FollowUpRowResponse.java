package com.sem.pmiautoevaluacion.pmi.dto;

import com.sem.pmiautoevaluacion.pmi.entity.ExecutionStatus;
import java.time.*;
import java.util.UUID;

public record FollowUpRowResponse(UUID id, UUID rowId, LocalDate controlDate,
        boolean editable, boolean sourceRemoved, boolean sourceChanged, FollowUpSourceResponse source,
        ExecutionStatus executionStatus, String executionPercentage, String observations, Instant updatedAt) {}
