package com.sem.pmiautoevaluacion.pmi.dto;

import java.time.LocalDate;
import java.util.UUID;

public record FollowUpSourceResponse(UUID componentId, UUID objectiveId, UUID goalId,
        String processName, String componentName, String objectiveDescription, String goalDescription,
        String indicatorName, String activities, LocalDate startDate, LocalDate finishDate) {}
