package com.sem.pmiautoevaluacion.selfEvaluation.state;

final class PertinenceState implements ValuationState {
    static final PertinenceState INSTANCE = new PertinenceState();
    private PertinenceState() {}

    @Override public ComponentValue getValue() { return ComponentValue.PERTINENCE; }
    @Override public boolean isImprovementChance() { return true; }
    @Override public boolean isStrength() { return false; }
    @Override public boolean allowsStrengths() { return true; }
    @Override public void countInto(ValuationTotals totals) { totals.countPertinence(); }
}
