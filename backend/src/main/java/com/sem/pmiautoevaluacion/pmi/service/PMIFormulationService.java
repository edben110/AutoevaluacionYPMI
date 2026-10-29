package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.entity.*;
import com.sem.pmiautoevaluacion.pmi.repository.ObjectiveRepository;
import com.sem.pmiautoevaluacion.pmi.repository.PMIRepository;
import com.sem.pmiautoevaluacion.pmi.repository.PrioritaryComponentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PMIFormulationService {
    private final SelfEvaluationRepository evaluations;
    private final PMIRepository plans;
    private final PrioritaryComponentRepository priorities;
    private final ObjectiveRepository objectives;
    private final PMIService pmiService;
    private final Clock clock;

    public PMIFormulationService(SelfEvaluationRepository evaluations, PMIRepository plans,
            PrioritaryComponentRepository priorities, ObjectiveRepository objectives,
            PMIService pmiService, Clock selfEvaluationClock) {
        this.evaluations = evaluations;
        this.plans = plans;
        this.priorities = priorities;
        this.objectives = objectives;
        this.pmiService = pmiService;
        this.clock = selfEvaluationClock;
    }

    @Transactional(readOnly = true)
    public PMIFormulationResponse find(UUID establishmentId, int year, UUID areaId) {
        return response(context(establishmentId, year, areaId));
    }

    @Transactional
    public PMIFormulationResponse createObjective(UUID establishmentId, int year, UUID areaId, ObjectiveRequest request) {
        lockEvaluation(establishmentId, year);
        FormulationContext context = context(establishmentId, year, areaId);
        requireWritable(context);
        PrioritaryComponent component = requireSelected(context, request.componentId());
        Objective objective = new Objective(component);
        validateChildren(objective, request, true);
        updateStructure(objective, component, request);
        context.plan().touch(clock.instant());
        objectives.saveAndFlush(objective);
        return response(context(establishmentId, year, areaId));
    }

    @Transactional
    public PMIFormulationResponse updateObjective(UUID establishmentId, int year, UUID areaId, UUID objectiveId,
            ObjectiveRequest request) {
        lockEvaluation(establishmentId, year);
        FormulationContext context = context(establishmentId, year, areaId);
        Objective objective = findOwned(context, objectiveId);
        requireWritable(context);
        if (!isActive(objective, context)) {
            throw new BadRequestException("Este objetivo está conservado para consulta; selecciona nuevamente su componente para editarlo");
        }
        PrioritaryComponent component = requireSelected(context, request.componentId());
        validateChildren(objective, request, false);
        updateStructure(objective, component, request);
        context.plan().touch(clock.instant());
        objectives.saveAndFlush(objective);
        return response(context(establishmentId, year, areaId));
    }

    @Transactional
    public void deleteObjective(UUID establishmentId, int year, UUID areaId, UUID objectiveId) {
        lockEvaluation(establishmentId, year);
        FormulationContext context = context(establishmentId, year, areaId);
        Objective objective = findOwned(context, objectiveId);
        PMIAccessService.requireDraft(context.plan());
        // Una eliminación explícita sigue disponible para gestionar objetivos históricos inactivos en borradores.
        context.plan().touch(clock.instant());
        objectives.delete(objective);
        objectives.flush();
    }

    private void lockEvaluation(UUID establishmentId, int year) {
        validateYear(year);
        evaluations.findForPmiUpdate(establishmentId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación de origen no encontrada"));
    }

    private FormulationContext context(UUID establishmentId, int year, UUID areaId) {
        PMIResponse source = pmiService.find(establishmentId, year, areaId);
        PMI plan = source.id() == null ? null : plans.findById(source.id()).orElseThrow();
        return new FormulationContext(source, plan,
                plan == null ? List.of() : priorities.findByPmi_Id(plan.getId()),
                plan == null ? List.of() : objectives.findByPrioritaryComponent_Pmi_IdOrderByIdAsc(plan.getId()));
    }

    private void requireWritable(FormulationContext context) {
        if (context.plan() == null || !context.source().canFormulate()) {
            throw new BadRequestException("Completa la autoevaluación y priorización del área y revisa sus máximos U/T/I antes de formular objetivos");
        }
    }

    private PrioritaryComponent requireSelected(FormulationContext context, UUID componentId) {
        return context.priorities().stream()
                .filter(item -> item.getValuation().getComponent().getId().equals(componentId))
                .filter(item -> context.source().selectedComponentIds().contains(componentId) && isEligible(context, componentId))
                .findFirst().orElseThrow(() -> new BadRequestException("El objetivo debe corresponder a un componente seleccionado y elegible del Anexo 2"));
    }

    private Objective findOwned(FormulationContext context, UUID objectiveId) {
        return context.objectives().stream().filter(item -> item.getId().equals(objectiveId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Objetivo de este PMI no encontrado"));
    }

    private boolean isActive(Objective objective, FormulationContext context) {
        PrioritaryComponent component = objective.getPrioritaryComponent();
        UUID componentId = component.getValuation().getComponent().getId();
        return context.source().selectedComponentIds().contains(componentId) && isEligible(context, componentId);
    }

    private boolean isEligible(FormulationContext context, UUID componentId) {
        return context.source().components().stream()
                .anyMatch(item -> item.componentId().equals(componentId) && item.eligible());
    }

    private void validateChildren(Objective objective, ObjectiveRequest request, boolean creating) {
        if (request == null || request.componentId() == null || request.goals() == null) {
            throw new BadRequestException("Indica el componente y la lista de metas del objetivo");
        }
        Map<UUID, Goal> existingGoals = objective.getGoals().stream()
                .collect(Collectors.toMap(Goal::getId, Function.identity()));
        Set<UUID> goalIds = new HashSet<>();
        Set<UUID> rowIds = new HashSet<>();
        for (FormulationGoalRequest goal : request.goals()) {
            if (goal == null || goal.rows() == null) {
                throw new BadRequestException("Cada meta debe incluir su lista de filas");
            }
            Goal existingGoal = goal.id() == null ? null : existingGoals.get(goal.id());
            if (goal.id() != null && (creating || !goalIds.add(goal.id()) || existingGoal == null)) {
                throw new BadRequestException("Las metas deben pertenecer al objetivo y no pueden repetir identificadores");
            }
            Set<UUID> existingRows = existingGoal == null ? Set.of() : existingGoal.getRows().stream()
                    .map(Activity::getId).collect(Collectors.toSet());
            for (FormulationRowRequest row : goal.rows()) {
                if (row == null) throw new BadRequestException("Las filas de una meta no pueden ser nulas");
                if (row.id() != null && (creating || !rowIds.add(row.id()) || !existingRows.contains(row.id()))) {
                    throw new BadRequestException("Las filas deben pertenecer a su meta y no pueden repetir identificadores");
                }
                if (row.startDate() != null && row.finishDate() != null && row.finishDate().isBefore(row.startDate())) {
                    throw new BadRequestException("La fecha de fin no puede ser anterior a la fecha de inicio");
                }
            }
        }
    }

    private void updateStructure(Objective objective, PrioritaryComponent component, ObjectiveRequest request) {
        objective.update(component, normalize(request.description()));
        Map<UUID, Goal> existingGoals = objective.getGoals().stream()
                .collect(Collectors.toMap(Goal::getId, Function.identity()));
        List<Goal> updatedGoals = new ArrayList<>();
        for (int goalIndex = 0; goalIndex < request.goals().size(); goalIndex++) {
            FormulationGoalRequest inputGoal = request.goals().get(goalIndex);
            Goal goal = inputGoal.id() == null ? new Goal(objective) : existingGoals.get(inputGoal.id());
            goal.update(normalize(inputGoal.description()), goalIndex);
            Map<UUID, Activity> existingRows = goal.getRows().stream()
                    .collect(Collectors.toMap(Activity::getId, Function.identity()));
            List<Activity> updatedRows = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < inputGoal.rows().size(); rowIndex++) {
                FormulationRowRequest inputRow = inputGoal.rows().get(rowIndex);
                Activity row = inputRow.id() == null ? new Activity(goal) : existingRows.get(inputRow.id());
                row.update(normalize(inputRow.indicatorName()), normalize(inputRow.activities()), inputRow.totalCost(),
                        inputRow.fseAmount(), inputRow.otherAmount(), normalize(inputRow.responsible()),
                        inputRow.startDate(), inputRow.finishDate(), normalize(inputRow.collectionInstruments()),
                        normalize(inputRow.collectionFrequency()), rowIndex);
                updatedRows.add(row);
            }
            goal.getRows().clear();
            goal.getRows().addAll(updatedRows);
            updatedGoals.add(goal);
        }
        // Solo la estructura enviada explícitamente sustituye hijos; los cambios del Anexo 2 nunca los borran.
        objective.getGoals().clear();
        objective.getGoals().addAll(updatedGoals);
    }

    private PMIFormulationResponse response(FormulationContext context) {
        Map<UUID, PriorityComponentResponse> byComponent = context.source().components().stream()
                .collect(Collectors.toMap(PriorityComponentResponse::componentId, Function.identity()));
        List<FormulationComponentResponse> selectedComponents = context.source().selectedComponentIds().stream()
                .map(byComponent::get)
                .map(item -> new FormulationComponentResponse(item.componentId(), item.componentName(), item.areaId(),
                        item.areaName(), item.processId(), item.processName(), item.eligible())).toList();
        List<ObjectiveResponse> items = context.objectives().stream().map(objective -> {
            var component = objective.getPrioritaryComponent().getValuation().getComponent();
            var process = component.getProcess();
            var area = process.getArea();
            return new ObjectiveResponse(objective.getId(), component.getId(), component.getName(), area.getId(),
                    area.getName(), process.getId(), process.getName(), objective.getDescription(),
                    isActive(objective, context) && context.source().canFormulate(),
                    objective.getGoals().stream().sorted(Comparator.comparingInt(Goal::getPosition))
                            .map(goal -> new FormulationGoalResponse(goal.getId(), goal.getDescription(),
                                    goal.getRows().stream().sorted(Comparator.comparingInt(Activity::getPosition))
                                            .map(this::rowResponse).toList())).toList());
        }).toList();
        PMIResponse source = context.source();
        return new PMIFormulationResponse(source.id(), source.year(), source.establishmentName(),
                source.projectionYears(), source.filledOn(), source.selectionNeedsReview(), selectedComponents,
                items, totals(context), source.areaId(), source.areaName(), source.selfEvaluationComplete(),
                source.prioritizationComplete(), source.canFormulate(), source.selfEvaluationSubmitted(), source.submittedAt(), context.plan() == null ? null : context.plan().getSubmittedAt());
    }

    private FormulationRowResponse rowResponse(Activity row) {
        return new FormulationRowResponse(row.getId(), row.getIndicator().getName(), row.getDescription(),
                money(row.getTotalCost()), money(row.getFseAmount()), money(row.getOtherAmount()), row.getResponsible(),
                row.getStartDate(), row.getFinishDate(), row.getCollectionInstruments(), row.getCollectionFrequency());
    }

    private FormulationTotalsResponse totals(FormulationContext context) {
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal fseAmount = BigDecimal.ZERO;
        BigDecimal otherAmount = BigDecimal.ZERO;
        int pending = 0;
        int excluded = 0;
        for (Objective objective : context.objectives()) {
            if (!isActive(objective, context)) {
                excluded++;
                continue;
            }
            for (Goal goal : objective.getGoals()) {
                for (Activity row : goal.getRows()) {
                    if (row.getTotalCost() == null) pending++; else totalCost = totalCost.add(row.getTotalCost());
                    if (row.getFseAmount() == null) pending++; else fseAmount = fseAmount.add(row.getFseAmount());
                    if (row.getOtherAmount() == null) pending++; else otherAmount = otherAmount.add(row.getOtherAmount());
                }
            }
        }
        return new FormulationTotalsResponse(money(totalCost), money(fseAmount), money(otherAmount), pending, excluded);
    }

    private String money(BigDecimal amount) { return amount == null ? null : amount.setScale(2).toPlainString(); }
    private String normalize(String text) { return text == null || text.isBlank() ? null : text.trim(); }
    private void validateYear(int year) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
    }

    private record FormulationContext(PMIResponse source, PMI plan,
            List<PrioritaryComponent> priorities, List<Objective> objectives) {}
}
