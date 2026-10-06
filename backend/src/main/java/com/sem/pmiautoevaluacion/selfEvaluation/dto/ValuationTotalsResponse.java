package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import com.sem.pmiautoevaluacion.selfEvaluation.state.ValuationTotals;

public record ValuationTotalsResponse(
        int existence,
        int pertinence,
        int appropriation,
        int continuousImprovement,
        int total
) {
    public static ValuationTotalsResponse from(ValuationTotals totals) {
        return new ValuationTotalsResponse(totals.getExistence(), totals.getPertinence(),
                totals.getAppropriation(), totals.getContinuousImprovement(), totals.getTotal());
    }
}
