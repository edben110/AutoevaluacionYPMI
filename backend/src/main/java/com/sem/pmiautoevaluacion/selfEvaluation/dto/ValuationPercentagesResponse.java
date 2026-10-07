package com.sem.pmiautoevaluacion.selfEvaluation.dto;

/** Porcentajes 0-100 de las valoraciones guardadas, redondeados a dos decimales. */
public record ValuationPercentagesResponse(
        double existence,
        double pertinence,
        double appropriation,
        double continuousImprovement
) {
    public static ValuationPercentagesResponse fromCounts(int existence, int pertinence,
            int appropriation, int continuousImprovement, int total) {
        return new ValuationPercentagesResponse(percent(existence, total), percent(pertinence, total),
                percent(appropriation, total), percent(continuousImprovement, total));
    }

    private static double percent(int count, int total) {
        return total == 0 ? 0 : Math.round(count * 10000.0 / total) / 100.0;
    }
}
