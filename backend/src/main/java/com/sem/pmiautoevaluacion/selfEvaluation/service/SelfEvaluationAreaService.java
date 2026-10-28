package com.sem.pmiautoevaluacion.selfEvaluation.service;

import com.sem.pmiautoevaluacion.integralManagement.entity.Area;
import com.sem.pmiautoevaluacion.integralManagement.repository.AreaRepository;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationAreaResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluationAreaSubmission;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluationPeriod;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.ComponentValuationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationAreaSubmissionRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationPeriodRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.pmi.service.PMIFollowUpCycleService;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfEvaluationAreaService {
    private final SelfEvaluationRepository evaluations;
    private final ComponentValuationRepository valuations;
    private final ComponentRepository components;
    private final AreaRepository areas;
    private final SelfEvaluationPeriodRepository periods;
    private final SelfEvaluationAreaSubmissionRepository submissions;
    private final Clock clock;
    private final PMIFollowUpCycleService followUpCycle;

    public SelfEvaluationAreaService(SelfEvaluationRepository evaluations, ComponentValuationRepository valuations,
            ComponentRepository components, AreaRepository areas, SelfEvaluationPeriodRepository periods,
            SelfEvaluationAreaSubmissionRepository submissions, Clock selfEvaluationClock, PMIFollowUpCycleService followUpCycle) {
        this.evaluations = evaluations;
        this.valuations = valuations;
        this.components = components;
        this.areas = areas;
        this.periods = periods;
        this.submissions = submissions;
        this.clock = selfEvaluationClock;
        this.followUpCycle = followUpCycle;
    }

    @Transactional(readOnly = true)
    public SelfEvaluationAreaResponse find(UUID establishmentId, int year, UUID areaId) {
        validateYear(year);
        SelfEvaluation evaluation = evaluations.findByEstablishment_IdAndYear(establishmentId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación de origen no encontrada"));
        Area area = requireArea(areaId);
        var submission = submissions.findByEvaluation_IdAndArea_Id(evaluation.getId(), areaId).orElse(null);
        return response(evaluation, area, submission, periods.findById(year).orElse(null));
    }

    @Transactional
    public SelfEvaluationAreaResponse submit(UUID establishmentId, int year, UUID areaId) {
        validateYear(year);
        // Mantiene el orden de bloqueo de guardar valoraciones: período y después autoevaluación.
        SelfEvaluationPeriod period = periods.findForDiligence(year).orElse(null);
        followUpCycle.requireCompleted(establishmentId, year);
        SelfEvaluation evaluation = evaluations.findForPmiUpdate(establishmentId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación de origen no encontrada"));
        Area area = requireArea(areaId);
        SelfEvaluationAreaSubmission existing = submissions
                .findByEvaluation_IdAndArea_Id(evaluation.getId(), areaId).orElse(null);
        // Un reintento conserva el envío original incluso después del cierre o retirada del período.
        if (existing != null) return response(evaluation, area, existing, period);
        if (period == null || !period.getPeriod().include(LocalDate.now(clock))) {
            throw new SelfEvaluationPeriodClosedException("El envío requiere un período de autoevaluación abierto");
        }
        SelfEvaluationAreaResponse current = response(evaluation, area, null, period);
        if (!current.complete()) {
            throw new BadRequestException("Valora todos los componentes activos del área antes de enviarla");
        }
        SelfEvaluationAreaSubmission submission = submissions.save(
                new SelfEvaluationAreaSubmission(evaluation, area, clock.instant().truncatedTo(ChronoUnit.MICROS)));
        evaluation.touch();
        return response(evaluation, area, submission, period);
    }

    private SelfEvaluationAreaResponse response(SelfEvaluation evaluation, Area area,
            SelfEvaluationAreaSubmission submission, SelfEvaluationPeriod period) {
        Set<UUID> activeIds = components.findByState(UseState.ACTIVE).stream()
                .filter(component -> component.getProcess().getState() == UseState.ACTIVE)
                .filter(component -> component.getProcess().getArea().getState() == UseState.ACTIVE)
                .filter(component -> component.getProcess().getArea().getId().equals(area.getId()))
                .map(component -> component.getId()).collect(Collectors.toSet());
        int valued = (int) valuations.findByEvaluation_Id(evaluation.getId()).stream()
                .filter(valuation -> activeIds.contains(valuation.getComponent().getId())).count();
        boolean complete = !activeIds.isEmpty() && valued == activeIds.size();
        boolean open = period != null && period.getPeriod().include(LocalDate.now(clock));
        return new SelfEvaluationAreaResponse(evaluation.getYear(), area.getId(), area.getName(),
                submission == null ? "DRAFT" : "SUBMITTED", submission == null ? null : submission.getSubmittedAt(),
                activeIds.size(), valued, complete, submission == null && complete && open
                        && followUpCycle.prerequisites(evaluation.getEstablishment().getId(), evaluation.getYear()).ready());
    }

    private Area requireArea(UUID areaId) {
        return areas.findById(areaId).orElseThrow(() -> new ResourceNotFoundException("Área de gestión no encontrada"));
    }

    private void validateYear(int year) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
    }
}
