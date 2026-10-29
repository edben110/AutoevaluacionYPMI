package com.sem.pmiautoevaluacion.pmi.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PMISubmissionResponse(UUID pmiId, int year, UUID areaId, String areaName,
        String status, Instant submittedAt, boolean canSubmit, List<String> pending) {}
