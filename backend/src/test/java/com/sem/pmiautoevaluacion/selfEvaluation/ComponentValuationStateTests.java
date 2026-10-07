package com.sem.pmiautoevaluacion.selfEvaluation;

import com.sem.pmiautoevaluacion.integralManagement.entity.Component;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationTotalsResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.ComponentValuation;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.state.ValuationTotals;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ComponentValuationStateTests {
    @ParameterizedTest
    @CsvSource({
            "1, EXISTENCE, Existencia, true, false, false, true",
            "2, PERTINENCE, Pertinencia, true, false, true, true",
            "3, APPROPRIATION, Apropiación, false, true, true, true",
            "4, CONTINUOUS_IMPROVEMENT, Mejoramiento continuo, false, true, true, false"
    })
    void restoresStateFromExistingNumericRecords(short level, String code, String label,
            boolean improvementChance, boolean strength, boolean strengthsAllowed,
            boolean improvementOpportunitiesAllowed) {
        ComponentValuation valuation = draftValuation();
        // Simula hidratación JPA: los registros anteriores solo tienen level y ninguna instancia State.
        ReflectionTestUtils.setField(valuation, "level", level);

        ValuationResponse response = ValuationResponse.from(valuation);

        assertEquals(level, response.level());
        assertEquals(code, response.state());
        assertEquals(label, response.stateLabel());
        assertEquals(improvementChance, response.improvementChance());
        assertEquals(strength, response.strength());
        assertEquals(strengthsAllowed, response.strengthsAllowed());
        assertEquals(improvementOpportunitiesAllowed, response.improvementOpportunitiesAllowed());
        assertEquals(code, valuation.getValuationState().getValue().name());
        ValuationTotals totals = new ValuationTotals();
        valuation.countInto(totals);
        assertEquals(1, totals.getTotal());
    }

    @Test
    void institutionCanSelectAnyStateDirectlyInEitherDirection() {
        for (short previous = 1; previous <= 4; previous++) {
            for (short selected = 1; selected <= 4; selected++) {
                ComponentValuation valuation = valued(previous);
                valuation.update(selected, "https://ejemplo.org/nueva-evidencia", "Nueva evidencia", null, null);

                assertEquals(selected, valuation.getLevel());
                assertEquals(selected, valuation.getValuationState().level());
                assertEquals("Nueva evidencia", valuation.getEvidenceNote());
                assertEquals("https://ejemplo.org/nueva-evidencia", valuation.getEvidenceUrl());
                assertNotNull(valuation.getUpdatedAt());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(shorts = {-1, 0, 5})
    void invalidSelectionPreservesPreviousStateAndEvidence(short selected) {
        ComponentValuation valuation = valued((short) 3);
        var updatedAt = valuation.getUpdatedAt();

        assertThrows(BadRequestException.class, () -> valuation.update(selected, "otro", "otra nota", "otra", "otra"));

        assertEquals(3, valuation.getLevel());
        assertEquals("APPROPRIATION", valuation.getValuationState().code());
        assertEquals("Evidencia original", valuation.getEvidenceNote());
        assertEquals(updatedAt, valuation.getUpdatedAt());
    }

    @Test
    void totalsCountEachComponentOnceWithoutWeightingByLevel() {
        ValuationTotals totals = new ValuationTotals();
        for (short level : new short[]{1, 3, 1, 4, 2, 3}) valued(level).countInto(totals);

        assertEquals(new ValuationTotalsResponse(2, 1, 2, 1, 6), ValuationTotalsResponse.from(totals));
        assertEquals(new ValuationTotalsResponse(0, 0, 0, 0, 0),
                ValuationTotalsResponse.from(new ValuationTotals()));
    }

    @Test
    void recountAfterManualCorrectionUsesOnlyCurrentState() {
        ComponentValuation valuation = valued((short) 1);
        ValuationTotals original = new ValuationTotals();
        valuation.countInto(original);

        valuation.update((short) 4, null, "Corrección manual", null, null);
        ValuationTotals corrected = new ValuationTotals();
        valuation.countInto(corrected);

        assertEquals(new ValuationTotalsResponse(1, 0, 0, 0, 1), ValuationTotalsResponse.from(original));
        assertEquals(new ValuationTotalsResponse(0, 0, 0, 1, 1), ValuationTotalsResponse.from(corrected));
    }

    private static ComponentValuation draftValuation() {
        return new ComponentValuation(new SelfEvaluation(null, 2026), new Component("Componente", ""));
    }

    private static ComponentValuation valued(short level) {
        ComponentValuation valuation = draftValuation();
        valuation.update(level, null, "Evidencia original", null, null);
        return valuation;
    }
}
