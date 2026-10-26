package com.sem.pmiautoevaluacion.pmi.dto;

import java.time.LocalDate;
import java.util.*;

public record PMIFollowUpResponse(UUID pmiId, int year, UUID areaId, String areaName,
        String establishmentName, LocalDate controlDate, List<LocalDate> availableDates,
        boolean canEdit, List<FollowUpRowResponse> rows) {}
