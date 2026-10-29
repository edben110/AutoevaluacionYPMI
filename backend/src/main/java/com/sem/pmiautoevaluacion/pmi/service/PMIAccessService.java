package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.pmi.dto.PMIOverviewResponse;
import com.sem.pmiautoevaluacion.pmi.entity.PMI;
import com.sem.pmiautoevaluacion.pmi.repository.PMIRepository;
import com.sem.pmiautoevaluacion.shared.exception.*;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PMIAccessService {
    private final PMIRepository plans;
    public PMIAccessService(PMIRepository plans) { this.plans = plans; }

    @Transactional(readOnly = true)
    public void requireSubmitted(UUID institutionId, int year, UUID areaId) {
        plans.findBySelfEvaluation_Establishment_IdAndSelfEvaluation_YearAndArea_Id(institutionId, year, areaId)
                .filter(PMI::isSubmitted)
                .orElseThrow(() -> new ResourceNotFoundException("La institución aún no ha enviado el PMI de esta área"));
    }

    @Transactional(readOnly = true)
    public PMIOverviewResponse submittedOverview(UUID institutionId, PMIOverviewResponse overview) {
        var visible = plans.findBySelfEvaluation_Establishment_IdAndSelfEvaluation_YearAndSubmittedAtIsNotNull(institutionId, overview.year())
                .stream().map(p -> p.getArea().getId()).collect(Collectors.toSet());
        return new PMIOverviewResponse(overview.year(), overview.areas().stream().filter(a -> visible.contains(a.areaId())).toList(), overview.periodConfigured());
    }

    public static void requireDraft(PMI plan) {
        if (plan != null && plan.isSubmitted())
            throw new BadRequestException("Este PMI ya fue enviado a Secretaría; los anexos 2, 3 y 4 son de consulta");
    }
}
