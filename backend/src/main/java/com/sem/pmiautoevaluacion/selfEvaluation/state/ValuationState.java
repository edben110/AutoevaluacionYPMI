package com.sem.pmiautoevaluacion.selfEvaluation.state;

/** Estado elegido manualmente por la institución; cada estado aporta a su propio subtotal. */
public sealed interface ValuationState permits ExistenceState, PertinenceState,
        AppropriationState, ContinuousImprovementState {
    ComponentValue getValue();
    boolean isImprovementChance();
    boolean isStrength();
    // Aptitud de los textos institucionales confirmada por Juan; la clasificación del PMI es independiente.
    default boolean allowsStrengths() { return isStrength(); }
    default boolean allowsImprovementOpportunities() { return isImprovementChance(); }
    default short level() { return getValue().getLevel(); }
    default String code() { return getValue().name(); }
    default String label() { return getValue().getLabel(); }
    void countInto(ValuationTotals totals);

    static ValuationState fromLevel(int level) {
        return switch (ComponentValue.fromLevel(level)) {
            case EXISTENCE -> ExistenceState.INSTANCE;
            case PERTINENCE -> PertinenceState.INSTANCE;
            case APPROPRIATION -> AppropriationState.INSTANCE;
            case CONTINUOUS_IMPROVEMENT -> ContinuousImprovementState.INSTANCE;
        };
    }
}
