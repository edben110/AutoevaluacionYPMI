package com.sem.pmiautoevaluacion.selfEvaluation.state;

final class PertinenceState implements ValuationState {
    static final PertinenceState INSTANCE = new PertinenceState();
    private PertinenceState() {}

    @Override public short level() { return 2; }
    @Override public String code() { return "PERTINENCE"; }
    @Override public String label() { return "Pertinencia"; }
    @Override public void countInto(ValuationTotals totals) { totals.countPertinence(); }
}
