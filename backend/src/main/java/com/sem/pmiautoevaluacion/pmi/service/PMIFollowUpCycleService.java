package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.entity.*;
import com.sem.pmiautoevaluacion.pmi.repository.*;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.*;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PMIFollowUpCycleService {
    private final SelfEvaluationRepository evaluations;
    private final SelfEvaluationPeriodRepository periods;
    private final PMIRepository plans;
    private final PMIFollowUpCompletionRepository completions;
    private final PMIFollowUpSourceService sources;
    private final Clock clock;
    public PMIFollowUpCycleService(SelfEvaluationRepository evaluations, SelfEvaluationPeriodRepository periods,
            PMIRepository plans, PMIFollowUpCompletionRepository completions, PMIFollowUpSourceService sources, Clock selfEvaluationClock) {
        this.evaluations = evaluations; this.periods = periods; this.plans = plans;
        this.completions = completions; this.sources = sources; this.clock = selfEvaluationClock;
    }
    @Transactional(readOnly = true)
    public SelfEvaluationPrerequisiteResponse prerequisites(UUID institutionId, int year) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
        var previous = year == 1 ? Optional.<com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation>empty()
                : evaluations.findByEstablishment_IdAndYear(institutionId, year - 1);
        List<PreviousFollowUpResponse> required = previous.isEmpty() ? List.of() : plans.findAllBySelfEvaluation_Id(previous.get().getId())
                .stream().sorted(Comparator.comparing(p -> p.getArea().getId())).map(plan -> {
                    var rows = sources.rows(plan.getId());
                    var done = completion(plan.getId(), rows);
                    return new PreviousFollowUpResponse(plan.getId(), year - 1, plan.getArea().getId(), plan.getArea().getName(),
                            done.isPresent(), done.map(PMIFollowUpCompletion::getCompletedAt).orElse(null), rows.size());
                }).toList();
        return new SelfEvaluationPrerequisiteResponse(year, year - 1, required.stream().allMatch(PreviousFollowUpResponse::completed), required);
    }
    @Transactional
    public void requireCompleted(UUID institutionId, int year) {
        // Orden de bloqueo: período del año nuevo (llamador), evaluación anterior y evaluación nueva.
        if (year > 1) evaluations.findForPmiUpdate(institutionId, year - 1);
        if (!prerequisites(institutionId, year).ready()) {
            throw new BadRequestException("Finaliza el seguimiento del PMI de " + (year - 1) + " en todas sus áreas antes de diligenciar la autoevaluación de " + year);
        }
    }
    public Optional<PMIFollowUpCompletion> completion(UUID planId, Map<UUID, FollowUpSnapshot> rows) {
        if (planId == null) return Optional.empty();
        return completions.findByPmi_Id(planId).filter(item -> item.getSourceFingerprint().equals(sources.fingerprint(rows)));
    }
    public String phase(int year, boolean completed) {
        if (completed) return "COMPLETED";
        if (year >= 9999) return "PLANNING";
        var period = periods.findById(year + 1).orElse(null);
        LocalDate today = LocalDate.now(clock);
        if (period == null || today.isBefore(period.getPeriod().getStartDate())) return "PLANNING";
        return period.getPeriod().include(today) ? "FOLLOW_UP" : "CLOSED";
    }
}
