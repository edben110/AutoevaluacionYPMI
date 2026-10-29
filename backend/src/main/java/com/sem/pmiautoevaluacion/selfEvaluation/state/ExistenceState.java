package com.sem.pmiautoevaluacion.selfEvaluation.state;

final class ExistenceState implements ValuationState {
    static final ExistenceState INSTANCE = new ExistenceState();
    private ExistenceState() {}

    @Override public ComponentValue getValue() { return ComponentValue.EXISTENCE; }
    @Override public boolean isImprovementChance() { return true; }
    @Override public boolean isStrength() { return false; }
    @Override public void countInto(ValuationTotals totals) { totals.countExistence(); }
}
