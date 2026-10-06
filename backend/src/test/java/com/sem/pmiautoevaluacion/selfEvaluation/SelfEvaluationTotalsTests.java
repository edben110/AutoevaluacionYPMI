package com.sem.pmiautoevaluacion.selfEvaluation;

import com.sem.pmiautoevaluacion.integralManagement.entity.Component;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationTotalsResponse;
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
                mock(EstablishmentRepository.class), mock(ComponentRepository.class));
        UUID institutionId = UUID.randomUUID();
        UUID evaluationId = UUID.randomUUID();
        SelfEvaluation evaluation = new SelfEvaluation(null, 2026);
        ReflectionTestUtils.setField(evaluation, "id", evaluationId);
        when(evaluations.findByEstablishment_IdAndYear(institutionId, 2026)).thenReturn(Optional.of(evaluation));
        Component component = new Component("Componente", "", "");
        UUID componentId = UUID.randomUUID();
        ReflectionTestUtils.setField(component, "id", componentId);
        ComponentValuation valuation = new ComponentValuation(evaluation, component);
        valuation.update((short) 1, null, null);
        when(valuations.findByEvaluation_Id(evaluationId)).thenReturn(List.of(valuation));

        var original = service.find(institutionId, 2026);
        assertEquals(new ValuationTotalsResponse(1, 0, 0, 0, 1), original.totals());
        assertEquals(componentId, original.valuations().getFirst().componentId());

        valuation.update((short) 4, null, "Cambio decidido por la institución");
        var corrected = service.find(institutionId, 2026);
        assertEquals(new ValuationTotalsResponse(0, 0, 0, 1, 1), corrected.totals());
        assertEquals("CONTINUOUS_IMPROVEMENT", corrected.valuations().getFirst().state());

        when(valuations.findByEvaluation_Id(evaluationId)).thenReturn(List.of());
        assertEquals(new ValuationTotalsResponse(0, 0, 0, 0, 0), service.find(institutionId, 2026).totals());
        verify(evaluations, times(3)).findByEstablishment_IdAndYear(institutionId, 2026);
        verify(valuations, times(3)).findByEvaluation_Id(evaluationId);
    }
}
