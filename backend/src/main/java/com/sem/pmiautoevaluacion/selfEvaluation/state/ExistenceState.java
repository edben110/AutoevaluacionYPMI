package com.sem.pmiautoevaluacion.selfEvaluation.state;

final class ExistenceState implements ValuationState {
    static final ExistenceState INSTANCE = new ExistenceState();
    private ExistenceState() {}

    @Override public short level() { return 1; }
    @Override public String code() { return "EXISTENCE"; }
    @Override public String label() { return "Existencia"; }
    @Override public void countInto(ValuationTotals totals) { totals.countExistence(); }
}
