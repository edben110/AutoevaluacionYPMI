package com.sem.pmiautoevaluacion.selfEvaluation.state;

final class ContinuousImprovementState implements ValuationState {
    static final ContinuousImprovementState INSTANCE = new ContinuousImprovementState();
    private ContinuousImprovementState() {}

    @Override public ComponentValue getValue() { return ComponentValue.CONTINUOUS_IMPROVEMENT; }
    @Override public boolean isImprovementChance() { return false; }
    @Override public boolean isStrength() { return true; }
    @Override public void countInto(ValuationTotals totals) { totals.countContinuousImprovement(); }
}
