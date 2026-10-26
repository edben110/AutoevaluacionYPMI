package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.entity.*;
import com.sem.pmiautoevaluacion.pmi.repository.*;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.shared.exception.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PMIFollowUpService {
    private final PMIFormulationService formulation;
    private final PMIFollowUpRepository followUps;
    private final PMIRepository plans;
    private final SelfEvaluationRepository evaluations;
    private final Clock clock;

    public PMIFollowUpService(PMIFormulationService formulation, PMIFollowUpRepository followUps,
            PMIRepository plans, SelfEvaluationRepository evaluations, Clock selfEvaluationClock) {
        this.formulation = formulation; this.followUps = followUps; this.plans = plans;
        this.evaluations = evaluations; this.clock = selfEvaluationClock;
    }

    @Transactional(readOnly = true)
    public PMIFollowUpResponse find(UUID institutionId, int year, UUID areaId, LocalDate requestedDate) {
        LocalDate date = requestedDate == null ? LocalDate.now(clock) : requestedDate;
        validateDate(date);
        PMIFormulationResponse source = formulation.find(institutionId, year, areaId);
        Map<UUID, SourceRow> current = sourceRows(source);
        Map<UUID, PMIFollowUp> saved = source.pmiId() == null ? Map.of() : followUps
                .findByPmi_IdAndControlDateOrderByIdAsc(source.pmiId(), date).stream()
                .collect(Collectors.toMap(PMIFollowUp::getSourceRowId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        LinkedHashSet<UUID> ids = new LinkedHashSet<>(current.keySet());
        ids.addAll(saved.keySet());
        List<FollowUpRowResponse> rows = ids.stream()
                .filter(id -> saved.containsKey(id) || current.get(id).hasContent())
                .map(id -> response(id, date, current.get(id), saved.get(id))).toList();
        return new PMIFollowUpResponse(source.pmiId(), year, areaId, source.areaName(), source.establishmentName(),
                date, source.pmiId() == null ? List.of() : followUps.findControlDates(source.pmiId()), source.canFormulate(), rows);
    }

    @Transactional
    public FollowUpRowResponse save(UUID institutionId, int year, UUID areaId, LocalDate date, UUID rowId, FollowUpRequest request) {
        validateDate(date);
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
        evaluations.findForPmiUpdate(institutionId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación de origen no encontrada"));
        PMIFormulationResponse source = formulation.find(institutionId, year, areaId);
        Map<UUID, SourceRow> current = sourceRows(source);
        SourceRow row = current.get(rowId);
        if (row == null) throw new ResourceNotFoundException("Fila del Anexo 3 de este PMI no encontrada");
        if (!row.editable() || !row.hasContent()) {
            throw new BadRequestException("Revisa la selección del Anexo 2 y guarda el indicador o las actividades en el Anexo 3 antes de registrar su seguimiento");
        }
        PMI plan = plans.findById(source.pmiId()).orElseThrow();
        PMIFollowUp item = followUps.findByPmi_IdAndControlDateAndSourceRowId(plan.getId(), date, rowId)
                .orElseGet(() -> new PMIFollowUp(plan, date, rowId, row.snapshot(), clock.instant()));
        // Actualizar un control corrige su avance, conservando el contexto con el que se registró.
        item.update(request.executionStatus(), request.executionPercentage(), normalize(request.observations()), clock.instant());
        plan.touch(clock.instant());
        followUps.saveAndFlush(item);
        return response(rowId, date, row, item);
    }

    private Map<UUID, SourceRow> sourceRows(PMIFormulationResponse source) {
        Map<UUID, SourceRow> result = new LinkedHashMap<>();
        for (ObjectiveResponse objective : source.objectives()) {
            for (FormulationGoalResponse goal : objective.goals()) {
                for (FormulationRowResponse row : goal.rows()) {
                    FollowUpSnapshot snapshot = new FollowUpSnapshot(objective.componentId(), objective.id(), goal.id(),
                            objective.processName(), objective.componentName(), objective.description(), goal.description(),
                            row.indicatorName(), row.activities(), row.startDate(), row.finishDate());
                    result.put(row.id(), new SourceRow(snapshot, source.canFormulate() && objective.editable(),
                            hasText(row.indicatorName()) || hasText(row.activities())));
                }
            }
        }
        return result;
    }

    private FollowUpRowResponse response(UUID rowId, LocalDate date, SourceRow current, PMIFollowUp saved) {
        FollowUpSourceResponse context = sourceResponse(saved == null ? current.snapshot() : saved.getSnapshot());
        boolean changed = saved != null && current != null && !context.equals(sourceResponse(current.snapshot()));
        return new FollowUpRowResponse(saved == null ? null : saved.getId(), rowId, date,
                current != null && current.editable() && current.hasContent(), current == null, changed, context,
                saved == null ? null : saved.getExecutionStatus(), saved == null || saved.getExecutionPercentage() == null ? null
                        : saved.getExecutionPercentage().setScale(2).toPlainString(),
                saved == null ? null : saved.getObservations(), saved == null ? null : saved.getUpdatedAt());
    }

    private FollowUpSourceResponse sourceResponse(FollowUpSnapshot s) {
        return new FollowUpSourceResponse(s.getComponentId(), s.getObjectiveId(), s.getGoalId(), s.getProcessName(),
                s.getComponentName(), s.getObjectiveDescription(), s.getGoalDescription(), s.getIndicatorName(),
                s.getActivities(), s.getStartDate(), s.getFinishDate());
    }
    private void validateDate(LocalDate date) {
        if (date == null || date.getYear() < 1 || date.getYear() > 9999) throw new BadRequestException("Indica una fecha de control válida");
    }
    private boolean hasText(String text) { return text != null && !text.isBlank(); }
    private String normalize(String text) { return hasText(text) ? text.trim() : null; }
    private record SourceRow(FollowUpSnapshot snapshot, boolean editable, boolean hasContent) {}
}
