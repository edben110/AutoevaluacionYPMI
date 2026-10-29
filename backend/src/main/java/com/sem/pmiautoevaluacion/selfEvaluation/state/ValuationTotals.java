package com.sem.pmiautoevaluacion.selfEvaluation.state;

/** Un componente guardado aporta una marca a un único estado, sin ponderar por su nivel. */
public final class ValuationTotals {
    private int existence;
    private int pertinence;
    private int appropriation;
    private int continuousImprovement;

    void countExistence() { existence++; }
    void countPertinence() { pertinence++; }
    void countAppropriation() { appropriation++; }
    void countContinuousImprovement() { continuousImprovement++; }

    public int getExistence() { return existence; }
    public int getPertinence() { return pertinence; }
    public int getAppropriation() { return appropriation; }
    public int getContinuousImprovement() { return continuousImprovement; }
    public int getTotal() { return existence + pertinence + appropriation + continuousImprovement; }
}
