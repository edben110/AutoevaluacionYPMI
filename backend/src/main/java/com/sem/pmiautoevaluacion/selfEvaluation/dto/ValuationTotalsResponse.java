package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import com.sem.pmiautoevaluacion.selfEvaluation.state.ValuationTotals;

public record ValuationTotalsResponse(
        int existence,
        int pertinence,
        int appropriation,
        int continuousImprovement,
        int total,
        ValuationPercentagesResponse percentages
) {
    public ValuationTotalsResponse(int existence, int pertinence, int appropriation,
            int continuousImprovement, int total) {
        this(existence, pertinence, appropriation, continuousImprovement, total,
                ValuationPercentagesResponse.fromCounts(existence, pertinence, appropriation,
                        continuousImprovement, total));
    }

    public static ValuationTotalsResponse from(ValuationTotals totals) {
        return new ValuationTotalsResponse(totals.getExistence(), totals.getPertinence(),
                totals.getAppropriation(), totals.getContinuousImprovement(), totals.getTotal());
    }
}
