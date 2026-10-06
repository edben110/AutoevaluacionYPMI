package com.sem.pmiautoevaluacion.selfEvaluation.state;

final class ContinuousImprovementState implements ValuationState {
    static final ContinuousImprovementState INSTANCE = new ContinuousImprovementState();
    private ContinuousImprovementState() {}

    @Override public short level() { return 4; }
    @Override public String code() { return "CONTINUOUS_IMPROVEMENT"; }
    @Override public String label() { return "Mejoramiento continuo"; }
    @Override public void countInto(ValuationTotals totals) { totals.countContinuousImprovement(); }
}
