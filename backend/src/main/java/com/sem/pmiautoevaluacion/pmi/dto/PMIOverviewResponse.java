package com.sem.pmiautoevaluacion.pmi.dto;

import java.util.List;

public record PMIOverviewResponse(int year, List<PMIAreaSummary> areas, boolean periodConfigured) {}
