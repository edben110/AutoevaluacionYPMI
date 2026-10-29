package com.sem.pmiautoevaluacion.selfEvaluation.state;

final class AppropriationState implements ValuationState {
    static final AppropriationState INSTANCE = new AppropriationState();
    private AppropriationState() {}

    @Override public ComponentValue getValue() { return ComponentValue.APPROPRIATION; }
    @Override public boolean isImprovementChance() { return false; }
    @Override public boolean isStrength() { return true; }
    @Override public boolean allowsImprovementOpportunities() { return true; }
    @Override public void countInto(ValuationTotals totals) { totals.countAppropriation(); }
}
