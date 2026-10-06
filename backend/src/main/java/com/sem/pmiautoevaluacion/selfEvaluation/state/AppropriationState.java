package com.sem.pmiautoevaluacion.selfEvaluation.state;

final class AppropriationState implements ValuationState {
    static final AppropriationState INSTANCE = new AppropriationState();
    private AppropriationState() {}

    @Override public short level() { return 3; }
    @Override public String code() { return "APPROPRIATION"; }
    @Override public String label() { return "Apropiación"; }
    @Override public void countInto(ValuationTotals totals) { totals.countAppropriation(); }
}
