package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.entity.*;
import com.sem.pmiautoevaluacion.pmi.repository.*;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationPeriodRepository;
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
    private final PMIFollowUpSourceService sources;
    private final PMIFollowUpCycleService cycle;
    private final PMIFollowUpCompletionRepository completions;
    private final SelfEvaluationPeriodRepository periods;

    public PMIFollowUpService(PMIFormulationService formulation, PMIFollowUpRepository followUps,
            PMIRepository plans, SelfEvaluationRepository evaluations, Clock selfEvaluationClock,
            PMIFollowUpSourceService sources, PMIFollowUpCycleService cycle,
            PMIFollowUpCompletionRepository completions, SelfEvaluationPeriodRepository periods) {
        this.formulation = formulation; this.followUps = followUps; this.plans = plans;
        this.evaluations = evaluations; this.clock = selfEvaluationClock;
        this.sources = sources; this.cycle = cycle; this.completions = completions; this.periods = periods;
    }

    @Transactional(readOnly = true)
    public PMIFollowUpResponse find(UUID institutionId, int year, UUID areaId, LocalDate requestedDate) {
        PMIFormulationResponse source = formulation.find(institutionId, year, areaId);
        Map<UUID, SourceRow> current = sourceRows(source);
        var done = cycle.completion(source.pmiId(), snapshots(current));
        LocalDate date = requestedDate == null ? done.map(PMIFollowUpCompletion::getControlDate).orElse(LocalDate.now(clock)) : requestedDate;
        validateDate(date);
        String phase = cycle.phase(year, done.isPresent());
        boolean writable = source.pmiId() != null && "FOLLOW_UP".equals(phase) && dateAllowed(year, date);
        Map<UUID, PMIFollowUp> saved = source.pmiId() == null ? Map.of() : followUps
                .findByPmi_IdAndControlDateOrderByIdAsc(source.pmiId(), date).stream()
                .collect(Collectors.toMap(PMIFollowUp::getSourceRowId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        LinkedHashSet<UUID> ids = new LinkedHashSet<>(current.keySet());
        ids.addAll(saved.keySet());
        List<FollowUpRowResponse> rows = ids.stream()
                .filter(id -> saved.containsKey(id) || current.get(id).hasContent())
                .map(id -> response(id, date, current.get(id), saved.get(id), writable)).toList();
        int completedRows = (int) current.entrySet().stream().filter(entry -> complete(saved.get(entry.getKey()), entry.getValue())).count();
        return new PMIFollowUpResponse(source.pmiId(), year, areaId, source.areaName(), source.establishmentName(),
                date, source.pmiId() == null ? List.of() : followUps.findControlDates(source.pmiId()), writable, rows,
                year + 1, phase, done.map(PMIFollowUpCompletion::getCompletedAt).orElse(null), current.size(), completedRows,
                writable && !current.isEmpty() && completedRows == current.size());
    }

    @Transactional
    public FollowUpRowResponse save(UUID institutionId, int year, UUID areaId, LocalDate date, UUID rowId, FollowUpRequest request) {
        validateDate(date);
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
        lockReview(institutionId, year);
        PMIFormulationResponse source = formulation.find(institutionId, year, areaId);
        Map<UUID, SourceRow> current = sourceRows(source);
        SourceRow row = current.get(rowId);
        if (row == null) throw new ResourceNotFoundException("Fila del Anexo 3 de este PMI no encontrada");
        if (!"FOLLOW_UP".equals(cycle.phase(year, cycle.completion(source.pmiId(), snapshots(current)).isPresent())) || !dateAllowed(year, date)) {
            throw new BadRequestException("El Anexo 5 se diligencia durante el período abierto de " + (year + 1) + ", antes de su autoevaluación. Usa una fecha de control dentro de ese período");
        }
        PMI plan = plans.findById(source.pmiId()).orElseThrow();
        PMIFollowUp item = followUps.findByPmi_IdAndControlDateAndSourceRowId(plan.getId(), date, rowId)
                .orElseGet(() -> new PMIFollowUp(plan, date, rowId, row.snapshot(), clock.instant()));
        // Actualizar un control corrige su avance, conservando el contexto con el que se registró.
        item.update(request.executionStatus(), request.executionPercentage(), normalize(request.observations()), clock.instant());
        plan.touch(clock.instant());
        followUps.saveAndFlush(item);
        return response(rowId, date, row, item, true);
    }

    @Transactional
    public PMIFollowUpResponse complete(UUID institutionId, int year, UUID areaId, LocalDate date) {
        validateDate(date);
        lockReview(institutionId, year);
        PMIFormulationResponse source = formulation.find(institutionId, year, areaId);
        if (source.pmiId() == null) throw new BadRequestException("No existe un PMI para finalizar su seguimiento");
        Map<UUID, SourceRow> rows = sourceRows(source);
        var done = cycle.completion(source.pmiId(), snapshots(rows));
        if (done.isPresent()) return find(institutionId, year, areaId, done.get().getControlDate());
        if (!"FOLLOW_UP".equals(cycle.phase(year, false)) || !dateAllowed(year, date)) {
            throw new BadRequestException("Abre el período de " + (year + 1) + " y usa una fecha de control dentro de sus fechas para finalizar el seguimiento");
        }
        Map<UUID, PMIFollowUp> saved = followUps.findByPmi_IdAndControlDateOrderByIdAsc(source.pmiId(), date).stream()
                .collect(Collectors.toMap(PMIFollowUp::getSourceRowId, Function.identity()));
        if (rows.isEmpty() || rows.entrySet().stream().anyMatch(entry -> !complete(saved.get(entry.getKey()), entry.getValue()))) {
            throw new BadRequestException("Guarda estado, porcentaje y observaciones de todas las filas en esta fecha. Si el plan cambió, registra un nuevo control con su contexto actualizado");
        }
        PMI plan = plans.findById(source.pmiId()).orElseThrow();
        PMIFollowUpCompletion item = completions.findByPmi_Id(plan.getId()).orElseGet(() -> new PMIFollowUpCompletion(plan));
        item.complete(year + 1, date, sources.fingerprint(snapshots(rows)), clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        completions.saveAndFlush(item);
        plan.touch(clock.instant());
        return find(institutionId, year, areaId, date);
    }

    private void lockReview(UUID institutionId, int year) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
        if (year < 9999) periods.findForDiligence(year + 1);
        evaluations.findForPmiUpdate(institutionId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación de origen no encontrada"));
    }

    private boolean dateAllowed(int year, LocalDate date) {
        return year < 9999 && periods.findById(year + 1).map(period -> period.getPeriod().include(date)).orElse(false);
    }

    private boolean complete(PMIFollowUp saved, SourceRow row) {
        return saved != null && saved.getExecutionStatus() != null && saved.getExecutionPercentage() != null
                && hasText(saved.getObservations()) && sources.response(saved.getSnapshot()).equals(sources.response(row.snapshot()));
    }

    private Map<UUID, FollowUpSnapshot> snapshots(Map<UUID, SourceRow> rows) {
        return rows.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().snapshot()));
    }

    private Map<UUID, SourceRow> sourceRows(PMIFormulationResponse source) {
        Map<UUID, SourceRow> result = new LinkedHashMap<>();
        sources.rows(source.pmiId()).forEach((id, snapshot) -> result.put(id, new SourceRow(snapshot, true, true)));
        return result;
    }

    private FollowUpRowResponse response(UUID rowId, LocalDate date, SourceRow current, PMIFollowUp saved, boolean writable) {
        FollowUpSourceResponse context = sourceResponse(saved == null ? current.snapshot() : saved.getSnapshot());
        boolean changed = saved != null && current != null && !context.equals(sourceResponse(current.snapshot()));
        return new FollowUpRowResponse(saved == null ? null : saved.getId(), rowId, date,
                writable && current != null, current == null, changed, context,
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
