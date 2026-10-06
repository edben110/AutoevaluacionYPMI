package com.sem.pmiautoevaluacion.selfEvaluation.state;

import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;

/** Estado elegido manualmente por la institución; cada estado aporta a su propio subtotal. */
public sealed interface ValuationState permits ExistenceState, PertinenceState,
        AppropriationState, ContinuousImprovementState {
    short level();
    String code();
    String label();
    void countInto(ValuationTotals totals);

    static ValuationState fromLevel(int level) {
        return switch (level) {
            case 1 -> ExistenceState.INSTANCE;
            case 2 -> PertinenceState.INSTANCE;
            case 3 -> AppropriationState.INSTANCE;
            case 4 -> ContinuousImprovementState.INSTANCE;
            default -> throw new BadRequestException("Selecciona un estado de valoración válido (1 a 4)");
        };
    }
}
