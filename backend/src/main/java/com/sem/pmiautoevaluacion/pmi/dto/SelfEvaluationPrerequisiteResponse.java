package com.sem.pmiautoevaluacion.pmi.dto;

import java.util.List;

public record SelfEvaluationPrerequisiteResponse(int year, int previousYear, boolean ready,
        List<PreviousFollowUpResponse> plans) {}
