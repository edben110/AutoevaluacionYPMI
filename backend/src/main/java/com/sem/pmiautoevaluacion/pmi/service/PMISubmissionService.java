package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.entity.PMI;
import com.sem.pmiautoevaluacion.pmi.repository.PMIRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.shared.exception.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PMISubmissionService {
    private final PMIService matrices;
    private final PMIFormulationService formulation;
    private final PMIIndicatorService indicators;
    private final PMIRepository plans;
    private final SelfEvaluationRepository evaluations;
    private final Clock clock;
    public PMISubmissionService(PMIService matrices, PMIFormulationService formulation, PMIIndicatorService indicators,
            PMIRepository plans, SelfEvaluationRepository evaluations, Clock selfEvaluationClock) {
        this.matrices = matrices; this.formulation = formulation; this.indicators = indicators;
        this.plans = plans; this.evaluations = evaluations; this.clock = selfEvaluationClock;
    }

    @Transactional(readOnly = true)
    public PMISubmissionResponse find(UUID institutionId, int year, UUID areaId) {
        var matrix = matrices.find(institutionId, year, areaId);
        PMI plan = matrix.id() == null ? null : plans.findById(matrix.id()).orElseThrow();
        if (plan != null && plan.isSubmitted()) return response(matrix, plan, List.of());
        List<String> pending = new ArrayList<>();
        if (!matrix.periodConfigured() || !matrix.selfEvaluationComplete() || !matrix.selfEvaluationSubmitted())
            pending.add("Autoevaluación: completa y envía el área dentro de un período publicado.");
        if (!matrix.prioritizationComplete() || matrix.selectedComponentIds().isEmpty() || matrix.selectionNeedsReview())
            pending.add("Anexo 2: guarda los factores y U/T/I de todos los candidatos y resuelve la selección de componentes.");
        if (plan == null) return response(matrix, null, pending);

        var form = formulation.find(institutionId, year, areaId);
        Set<UUID> activeObjectives = new HashSet<>();
        for (UUID componentId : matrix.selectedComponentIds()) {
            var objectives = form.objectives().stream().filter(o -> componentId.equals(o.componentId())).toList();
            if (objectives.isEmpty()) pending.add("Anexo 3: falta formular el plan de " + name(matrix, componentId) + ".");
            for (var objective : objectives) {
                activeObjectives.add(objective.id());
                String context = "Anexo 3 · " + objective.componentName() + ": ";
                if (!text(objective.description()) || objective.goals().isEmpty())
                    pending.add(context + "completa el objetivo y al menos una meta.");
                for (var goal : objective.goals()) {
                    if (!text(goal.description()) || goal.rows().isEmpty()) pending.add(context + "cada meta necesita descripción y al menos un indicador con actividades.");
                    for (var row : goal.rows()) {
                        List<String> missing = new ArrayList<>();
                        if (!text(row.indicatorName())) missing.add("nombre del indicador");
                        if (!text(row.activities())) missing.add("actividades");
                        if (row.totalCost() == null || row.fseAmount() == null || row.otherAmount() == null) missing.add("costo y fuentes de recursos (pueden ser cero)");
                        if (!text(row.responsible())) missing.add("responsable");
                        if (row.startDate() == null || row.finishDate() == null) missing.add("fechas de inicio y fin");
                        if (!text(row.collectionInstruments())) missing.add("instrumentos de recolección");
                        if (!text(row.collectionFrequency())) missing.add("frecuencia de recolección");
                        if (!missing.isEmpty()) pending.add(context + (text(row.indicatorName()) ? row.indicatorName() + ": " : "") + String.join(", ", missing) + ".");
                    }
                }
            }
        }
        for (var sheet : indicators.find(institutionId, year, areaId).indicators()) {
            if (activeObjectives.contains(sheet.objectiveId()) && !sheet.complete())
                pending.add("Anexo 4 · " + sheet.componentName() + ": completa la ficha de «" + sheet.name() + "».");
        }
        return response(matrix, plan, pending.stream().distinct().toList());
    }

    @Transactional
    public PMISubmissionResponse submit(UUID institutionId, int year, UUID areaId) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
        var evaluation = evaluations.findForPmiUpdate(institutionId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación no encontrada"));
        PMI plan = plans.findBySelfEvaluation_IdAndArea_Id(evaluation.getId(), areaId)
                .orElseThrow(() -> new BadRequestException("Completa el PMI antes de enviarlo"));
        if (!plan.isSubmitted()) {
            var current = find(institutionId, year, areaId);
            if (!current.canSubmit()) throw new BadRequestException("No se puede enviar el PMI. " + String.join(" ", current.pending()));
            plan.submit(clock.instant().truncatedTo(ChronoUnit.MICROS));
            plans.flush();
        }
        return find(institutionId, year, areaId);
    }

    private PMISubmissionResponse response(PMIResponse matrix, PMI plan, List<String> pending) {
        boolean sent = plan != null && plan.isSubmitted();
        return new PMISubmissionResponse(matrix.id(), matrix.year(), matrix.areaId(), matrix.areaName(),
                sent ? "SUBMITTED" : "DRAFT", sent ? plan.getSubmittedAt() : null,
                !sent && plan != null && pending.isEmpty(), pending);
    }
    private boolean text(String value) { return value != null && !value.isBlank(); }
    private String name(PMIResponse matrix, UUID componentId) {
        return matrix.components().stream().filter(c -> c.componentId().equals(componentId)).findFirst().orElseThrow().componentName();
    }
}
