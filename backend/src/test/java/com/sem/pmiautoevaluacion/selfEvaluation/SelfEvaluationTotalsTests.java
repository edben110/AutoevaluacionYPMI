package com.sem.pmiautoevaluacion.selfEvaluation;

import com.sem.pmiautoevaluacion.integralManagement.entity.Component;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationTotalsResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationPercentagesResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.ComponentValuation;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.ComponentValuationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SelfEvaluationTotalsTests {
    @Test
    void consultationCountsOnlySavedValuationsOwnedByInstitution() {
        var evaluations = mock(SelfEvaluationRepository.class);
        var valuations = mock(ComponentValuationRepository.class);
        var service = new SelfEvaluationService(evaluations, valuations,
                mock(EstablishmentRepository.class), mock(ComponentRepository.class),
                mock(com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationPeriodService.class));
        UUID institutionId = UUID.randomUUID();
        UUID evaluationId = UUID.randomUUID();
        SelfEvaluation evaluation = new SelfEvaluation(null, 2026);
        ReflectionTestUtils.setField(evaluation, "id", evaluationId);
        when(evaluations.findByEstablishment_IdAndYear(institutionId, 2026)).thenReturn(Optional.of(evaluation));
        Component component = new Component("Componente", "", "");
        UUID componentId = UUID.randomUUID();
        ReflectionTestUtils.setField(component, "id", componentId);
        ComponentValuation valuation = new ComponentValuation(evaluation, component);
        valuation.update((short) 1, null, null, null, null);
        when(valuations.findByEvaluation_Id(evaluationId)).thenReturn(List.of(valuation));

        var original = service.find(institutionId, 2026);
        assertEquals(new ValuationTotalsResponse(1, 0, 0, 0, 1), original.totals());
        assertEquals(componentId, original.valuations().getFirst().componentId());
        assertEquals(new ValuationPercentagesResponse(100, 0, 0, 0), original.totals().percentages());

        valuation.update((short) 4, null, "Cambio decidido por la institución", null, null);
        var corrected = service.find(institutionId, 2026);
        assertEquals(new ValuationTotalsResponse(0, 0, 0, 1, 1), corrected.totals());
        assertEquals("CONTINUOUS_IMPROVEMENT", corrected.valuations().getFirst().state());
        assertEquals(new ValuationPercentagesResponse(0, 0, 0, 100), corrected.totals().percentages());

        when(valuations.findByEvaluation_Id(evaluationId)).thenReturn(List.of());
        assertEquals(new ValuationTotalsResponse(0, 0, 0, 0, 0), service.find(institutionId, 2026).totals());
        verify(evaluations, times(3)).findByEstablishment_IdAndYear(institutionId, 2026);
        verify(valuations, times(3)).findByEvaluation_Id(evaluationId);
    }

    @Test
    void percentagesMatchExcelExampleAndHandleEmptyAndRounding() {
        // Anexo 1, C34:F35: 18 marcas, de las cuales 4 son P, 11 A y 3 M.C.
        assertEquals(new ValuationPercentagesResponse(0, 22.22, 61.11, 16.67),
                new ValuationTotalsResponse(0, 4, 11, 3, 18).percentages());
        assertEquals(new ValuationPercentagesResponse(0, 0, 0, 0),
                new ValuationTotalsResponse(0, 0, 0, 0, 0).percentages());
        assertEquals(new ValuationPercentagesResponse(33.33, 16.67, 33.33, 16.67),
                new ValuationTotalsResponse(2, 1, 2, 1, 6).percentages());
    }
}
