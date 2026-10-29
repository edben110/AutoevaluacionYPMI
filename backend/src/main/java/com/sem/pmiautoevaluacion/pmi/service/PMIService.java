package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.integralManagement.entity.Area;
import com.sem.pmiautoevaluacion.integralManagement.repository.AreaRepository;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.entity.PMI;
import com.sem.pmiautoevaluacion.pmi.entity.PrioritaryComponent;
import com.sem.pmiautoevaluacion.pmi.repository.PMIRepository;
import com.sem.pmiautoevaluacion.pmi.repository.PrioritaryComponentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.ComponentValuation;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.ComponentValuationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationAreaSubmissionRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.state.ValuationState;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PMIService {
    private final SelfEvaluationRepository evaluations;
    private final ComponentValuationRepository valuations;
    private final PMIRepository plans;
    private final PrioritaryComponentRepository priorities;
    private final AreaRepository areas;
    private final ComponentRepository components;
    private final SelfEvaluationAreaSubmissionRepository submissions;
    private final Clock clock;

    public PMIService(SelfEvaluationRepository evaluations, ComponentValuationRepository valuations,
            PMIRepository plans, PrioritaryComponentRepository priorities, AreaRepository areas,
            ComponentRepository components, SelfEvaluationAreaSubmissionRepository submissions, Clock selfEvaluationClock) {
        this.evaluations = evaluations;
        this.valuations = valuations;
        this.plans = plans;
        this.priorities = priorities;
        this.areas = areas;
        this.components = components;
        this.submissions = submissions;
        this.clock = selfEvaluationClock;
    }

    @Transactional(readOnly = true)
    public PMIOverviewResponse overview(UUID establishmentId, int year) {
        SelfEvaluation evaluation = findEvaluation(establishmentId, year, false);
        Set<UUID> historicalAreas = plans.findAllBySelfEvaluation_Id(evaluation.getId()).stream()
                .map(plan -> plan.getArea().getId()).collect(Collectors.toSet());
        return new PMIOverviewResponse(year, areas.findAll().stream()
                .filter(area -> area.getState() == UseState.ACTIVE || historicalAreas.contains(area.getId()))
                .sorted(Comparator.comparing(Area::getId)).map(area -> {
                    AreaAnalysis analysis = analyze(evaluation, area);
                    return new PMIAreaSummary(area.getId(), area.getName(), analysis.activeIds().size(),
                            analysis.valuedCount(), analysis.complete(), analysis.candidateLevel(),
                            stateLabel(analysis.candidateLevel()), analysis.candidateIds().size(),
                            analysis.submittedAt() != null, analysis.submittedAt());
                }).toList(), evaluation.getPeriod() != null);
    }

    @Transactional(readOnly = true)
    public PMIResponse find(UUID establishmentId, int year, UUID areaId) {
        SelfEvaluation evaluation = findEvaluation(establishmentId, year, false);
        AreaAnalysis analysis = analyze(evaluation, requireArea(areaId));
        PMI plan = plans.findBySelfEvaluation_IdAndArea_Id(evaluation.getId(), areaId).orElse(null);
        return response(analysis, plan, plan == null ? List.of() : priorities.findByPmi_Id(plan.getId()));
    }

    @Transactional
    public PMIResponse saveComponent(UUID establishmentId, int year, UUID areaId, UUID componentId,
            PriorityComponentRequest request) {
        SelfEvaluation evaluation = findEvaluation(establishmentId, year, true);
        Area area = requireArea(areaId);
        AreaAnalysis analysis = analyze(evaluation, area);
        requireComplete(analysis);
        ComponentValuation valuation = analysis.valuations().stream()
                .filter(item -> item.getComponent().getId().equals(componentId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Componente valorado en esta autoevaluación no encontrado"));
        if (!analysis.candidateIds().contains(valuation.getId())) {
            throw new BadRequestException("Prioriza únicamente los componentes del menor estado presente en esta área");
        }
        if (request == null || request.urgency() == null || request.tendency() == null || request.impact() == null) {
            throw new BadRequestException("Indica urgencia, tendencia e impacto entre 1 y 5");
        }
        PMI plan = plans.findBySelfEvaluation_IdAndArea_Id(evaluation.getId(), areaId).orElseGet(() ->
                plans.save(new PMI(evaluation, area, LocalDate.now(clock), clock.instant())));
        PMIAccessService.requireDraft(plan);
        List<PrioritaryComponent> saved = new ArrayList<>(priorities.findByPmi_Id(plan.getId()));
        PrioritaryComponent priority = saved.stream()
                .filter(item -> item.getValuation().getId().equals(valuation.getId())).findFirst()
                .orElseGet(() -> {
                    var created = new PrioritaryComponent(plan, valuation);
                    saved.add(created);
                    return created;
                });
        try {
            priority.update(request.criticalFactors(), request.urgency().intValueExact(),
                    request.tendency().intValueExact(), request.impact().intValueExact());
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Urgencia, tendencia e impacto deben ser números enteros entre 1 y 5");
        }
        priorities.save(priority);
        plan.touch(clock.instant());
        return response(analysis, plan, saved);
    }

    @Transactional
    public PMIResponse select(UUID establishmentId, int year, UUID areaId, PMISelectionRequest request) {
        SelfEvaluation evaluation = findEvaluation(establishmentId, year, true);
        AreaAnalysis analysis = analyze(evaluation, requireArea(areaId));
        requireComplete(analysis);
        if (request == null || request.componentIds() == null || request.componentIds().isEmpty()
                || request.componentIds().size() > 2 || request.componentIds().stream().anyMatch(Objects::isNull)
                || new HashSet<>(request.componentIds()).size() != request.componentIds().size()) {
            throw new BadRequestException("Selecciona componentes distintos de mayor puntuación U/T/I");
        }
        PMI plan = plans.findBySelfEvaluation_IdAndArea_Id(evaluation.getId(), areaId)
                .orElseThrow(() -> new BadRequestException("Completa la priorización del área antes de seleccionar"));
        PMIAccessService.requireDraft(plan);
        List<PrioritaryComponent> saved = priorities.findByPmi_Id(plan.getId());
        PMIResponse current = response(analysis, plan, saved);
        if (!current.prioritizationComplete()) {
            throw new BadRequestException("Guarda los factores y U/T/I de todos los candidatos del área");
        }
        int requiredCount = Math.min(2, current.highestComponentIds().size());
        if (request.componentIds().size() != requiredCount
                || !current.highestComponentIds().containsAll(request.componentIds())) {
            throw new BadRequestException(requiredCount == 1
                    ? "Solo el componente de mayor puntuación puede pasar a formulación"
                    : "Selecciona exactamente dos componentes empatados en la mayor puntuación U/T/I");
        }
        Map<UUID, PrioritaryComponent> byComponent = saved.stream().collect(Collectors.toMap(
                item -> item.getValuation().getComponent().getId(), Function.identity()));
        saved.forEach(item -> item.select(null));
        for (int index = 0; index < request.componentIds().size(); index++) {
            byComponent.get(request.componentIds().get(index)).select(index + 1);
        }
        plan.touch(clock.instant());
        return response(analysis, plan, saved);
    }

    private SelfEvaluation findEvaluation(UUID establishmentId, int year, boolean lock) {
        validateYear(year);
        return (lock ? evaluations.findForPmiUpdate(establishmentId, year)
                : evaluations.findByEstablishment_IdAndYear(establishmentId, year))
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación de origen no encontrada"));
    }

    private Area requireArea(UUID areaId) {
        return areas.findById(areaId).orElseThrow(() -> new ResourceNotFoundException("Área de gestión no encontrada"));
    }

    private AreaAnalysis analyze(SelfEvaluation evaluation, Area area) {
        Set<UUID> activeIds = components.findByState(UseState.ACTIVE).stream()
                .filter(component -> component.getProcess().getState() == UseState.ACTIVE)
                .filter(component -> component.getProcess().getArea().getState() == UseState.ACTIVE)
                .filter(component -> component.getProcess().getArea().getId().equals(area.getId()))
                .map(component -> component.getId()).collect(Collectors.toSet());
        List<ComponentValuation> all = valuations.findByEvaluation_Id(evaluation.getId());
        List<ComponentValuation> activeValuations = all.stream()
                .filter(item -> activeIds.contains(item.getComponent().getId())).toList();
        boolean complete = !activeIds.isEmpty() && activeValuations.size() == activeIds.size();
        int minimum = complete ? activeValuations.stream().mapToInt(ComponentValuation::getLevel).min().orElse(4) : 4;
        Integer candidateLevel = minimum < 4 ? minimum : null;
        Set<UUID> candidateIds = candidateLevel == null ? Set.of() : activeValuations.stream()
                .filter(item -> item.getLevel() == candidateLevel).map(ComponentValuation::getId).collect(Collectors.toSet());
        Instant submittedAt = submissions.findByEvaluation_IdAndArea_Id(evaluation.getId(), area.getId())
                .map(submission -> submission.getSubmittedAt()).orElse(null);
        return new AreaAnalysis(evaluation, area, all, activeIds, activeValuations.size(), complete,
                candidateLevel, candidateIds, submittedAt);
    }

    private void requireComplete(AreaAnalysis analysis) {
        if (analysis.evaluation().getPeriod() == null) {
            throw new BadRequestException("Secretaría debe publicar el período de este año para iniciar o guardar su PMI");
        }
        if (!analysis.complete()) {
            throw new BadRequestException("Valora todos los componentes activos del área antes de iniciar su PMI");
        }
        if (analysis.submittedAt() == null) {
            throw new BadRequestException("Envía la autoevaluación de esta área para habilitar su PMI");
        }
    }

    private PMIResponse response(AreaAnalysis analysis, PMI plan, List<PrioritaryComponent> saved) {
        Map<UUID, PrioritaryComponent> byValuation = saved.stream().collect(Collectors.toMap(
                item -> item.getValuation().getId(), Function.identity()));
        boolean prioritizationComplete = analysis.evaluation().getPeriod() != null && analysis.complete() && analysis.submittedAt() != null
                && !analysis.candidateIds().isEmpty()
                && byValuation.keySet().containsAll(analysis.candidateIds());
        List<PrioritaryComponent> candidates = saved.stream()
                .filter(item -> analysis.candidateIds().contains(item.getValuation().getId())).toList();
        int maximum = prioritizationComplete ? candidates.stream().mapToInt(PrioritaryComponent::getSumUTI).max().orElse(0) : 0;
        List<UUID> highest = prioritizationComplete ? candidates.stream().filter(item -> item.getSumUTI() == maximum)
                .map(item -> item.getValuation().getComponent().getId()).sorted().toList() : List.of();
        List<UUID> selected = List.of();
        boolean needsReview = false;
        if (prioritizationComplete) {
            if (highest.size() <= 2) selected = highest;
            else {
                List<UUID> stored = saved.stream().filter(item -> item.getSelectedSlot() != null)
                        .sorted(Comparator.comparing(PrioritaryComponent::getSelectedSlot))
                        .map(item -> item.getValuation().getComponent().getId()).toList();
                if (stored.size() == 2 && highest.containsAll(stored)) selected = stored;
                else needsReview = true;
            }
        }
        Set<UUID> selectedIds = new HashSet<>(selected);
        List<PriorityComponentResponse> items = analysis.valuations().stream()
                .filter(item -> analysis.candidateIds().contains(item.getId()) || byValuation.containsKey(item.getId()))
                .sorted(Comparator.comparing((ComponentValuation item) -> item.getComponent().getProcess().getId())
                        .thenComparing(item -> item.getComponent().getId()))
                .map(item -> componentResponse(item, byValuation.get(item.getId()),
                        analysis.candidateIds().contains(item.getId()), selectedIds.contains(item.getComponent().getId()))).toList();
        SelfEvaluation evaluation = analysis.evaluation();
        return new PMIResponse(plan == null ? null : plan.getId(), evaluation.getId(), evaluation.getYear(),
                plan != null && plan.isSubmitted() ? "SUBMITTED" : "DRAFT", plan == null ? 1 : plan.getProjectionYears(), plan == null ? null : plan.getFilledOn(),
                plan == null ? null : plan.getUpdatedAt(), evaluation.getEstablishment().getName(), items,
                highest, selected, needsReview, analysis.area().getId(), analysis.area().getName(), analysis.complete(),
                analysis.activeIds().size(), analysis.valuedCount(), analysis.candidateLevel(),
                stateLabel(analysis.candidateLevel()), prioritizationComplete,
                prioritizationComplete && !selected.isEmpty() && !needsReview && (plan == null || !plan.isSubmitted()),
                analysis.submittedAt() != null, analysis.submittedAt(), evaluation.getPeriod() != null);
    }

    private PriorityComponentResponse componentResponse(ComponentValuation valuation, PrioritaryComponent priority,
            boolean eligible, boolean selected) {
        var component = valuation.getComponent();
        var process = component.getProcess();
        var area = process.getArea();
        return new PriorityComponentResponse(component.getId(), valuation.getId(), area.getId(), area.getName(),
                process.getId(), process.getName(), component.getName(), valuation.getLevel(),
                valuation.getValuationState().code(), valuation.getValuationState().label(),
                valuation.getImprovementOpportunities(), eligible,
                priority == null ? null : priority.getCriticalFactors(), priority == null ? null : priority.getUrgency(),
                priority == null ? null : priority.getTendency(), priority == null ? null : priority.getImpact(),
                priority == null ? null : priority.getSumUTI(), selected);
    }

    private String stateLabel(Integer level) { return level == null ? null : ValuationState.fromLevel(level).label(); }
    private void validateYear(int year) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
    }
    private record AreaAnalysis(SelfEvaluation evaluation, Area area, List<ComponentValuation> valuations,
            Set<UUID> activeIds, int valuedCount, boolean complete, Integer candidateLevel, Set<UUID> candidateIds,
            Instant submittedAt) {}
}
