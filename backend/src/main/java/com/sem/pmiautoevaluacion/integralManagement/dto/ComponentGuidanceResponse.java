package com.sem.pmiautoevaluacion.integralManagement.dto;

import java.util.List;
import java.util.UUID;

public record ComponentGuidanceResponse(UUID componentId, String componentName, String areaName,
        String processName, String evaluationItem, List<String> evidenceExamples, String evidenceNotice,
        String sourceTitle, String sourceUrl, List<Integer> itemPages, List<Integer> evidencePages,
        boolean officialGuidance) {}
