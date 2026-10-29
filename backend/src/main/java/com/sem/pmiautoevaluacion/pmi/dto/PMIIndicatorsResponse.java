package com.sem.pmiautoevaluacion.pmi.dto;

import java.util.List;
import java.util.UUID;

public record PMIIndicatorsResponse(
        UUID pmiId, int year, UUID areaId, String areaName, String establishmentName,
        boolean canEdit, int unnamedRows, List<IndicatorSheetResponse> indicators, java.time.Instant pmiSubmittedAt) {}
