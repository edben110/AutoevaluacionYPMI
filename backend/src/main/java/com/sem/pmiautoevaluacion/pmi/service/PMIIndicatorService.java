package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.entity.*;
import com.sem.pmiautoevaluacion.pmi.repository.ObjectiveRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PMIIndicatorService {
    private final PMIFormulationService formulation;
    private final ObjectiveRepository objectives;
    private final SelfEvaluationRepository evaluations;
    private final Clock clock;

    public PMIIndicatorService(PMIFormulationService formulation, ObjectiveRepository objectives,
            SelfEvaluationRepository evaluations, Clock selfEvaluationClock) {
        this.formulation = formulation;
        this.objectives = objectives;
        this.evaluations = evaluations;
        this.clock = selfEvaluationClock;
    }

    @Transactional(readOnly = true)
    public PMIIndicatorsResponse find(UUID establishmentId, int year, UUID areaId) {
        PMIFormulationResponse source = formulation.find(establishmentId, year, areaId);
        List<Activity> rows = rows(source);
        List<IndicatorSheetResponse> indicators = rows.stream()
                .filter(row -> hasName(row.getIndicator()) || row.getIndicator().hasDefinition())
                .map(row -> response(row, source)).toList();
        int unnamed = (int) rows.stream().filter(row -> !hasName(row.getIndicator())).count();
        return new PMIIndicatorsResponse(source.pmiId(), year, areaId, source.areaName(),
                source.establishmentName(), source.canFormulate(), unnamed, indicators, source.pmiSubmittedAt());
    }

    @Transactional
    public IndicatorSheetResponse save(UUID establishmentId, int year, UUID areaId, UUID indicatorId,
            IndicatorDefinitionRequest request) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
        // Mismo bloqueo que Anexos 2 y 3: selección, renombrado, eliminación y ficha se coordinan.
        evaluations.findForPmiUpdate(establishmentId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación de origen no encontrada"));
        PMIFormulationResponse source = formulation.find(establishmentId, year, areaId);
        Activity row = rows(source).stream().filter(item -> item.getIndicator().getId().equals(indicatorId))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Indicador de este PMI no encontrado"));
        if (!isEditable(row, source)) {
            throw new BadRequestException("Revisa la selección del Anexo 2 y guarda el nombre del indicador en el Anexo 3 antes de editar su ficha");
        }
        Indicator indicator = row.getIndicator();
        indicator.define(request.type(), normalize(request.description()), normalize(request.measureUnit()),
                normalize(request.variablesDefinition()), normalize(request.dataFormula()), normalize(request.dataSource()),
                normalize(request.methodologicalAspects()), normalize(request.calculationFrequency()),
                normalize(request.measurementResponsible()));
        row.getGoal().getObjective().getPrioritaryComponent().getPmi().touch(clock.instant());
        objectives.flush();
        return response(row, source);
    }

    private List<Activity> rows(PMIFormulationResponse source) {
        if (source.pmiId() == null) return List.of();
        return objectives.findByPrioritaryComponent_Pmi_IdOrderByIdAsc(source.pmiId()).stream()
                .flatMap(objective -> objective.getGoals().stream().sorted(Comparator.comparingInt(Goal::getPosition)))
                .flatMap(goal -> goal.getRows().stream().sorted(Comparator.comparingInt(Activity::getPosition))).toList();
    }

    private boolean isEditable(Activity row, PMIFormulationResponse source) {
        UUID objectiveId = row.getGoal().getObjective().getId();
        return source.canFormulate() && hasName(row.getIndicator()) && source.objectives().stream()
                .anyMatch(objective -> objective.id().equals(objectiveId) && objective.editable());
    }

    private IndicatorSheetResponse response(Activity row, PMIFormulationResponse source) {
        Goal goal = row.getGoal();
        Objective objective = goal.getObjective();
        var component = objective.getPrioritaryComponent().getValuation().getComponent();
        Indicator indicator = row.getIndicator();
        return new IndicatorSheetResponse(indicator.getId(), row.getId(), objective.getId(), goal.getId(),
                component.getId(), component.getName(), component.getProcess().getName(),
                objective.getDescription(), goal.getDescription(), indicator.getName(), isEditable(row, source),
                indicator.isDefinitionComplete(), new IndicatorDefinitionResponse(indicator.getType(),
                indicator.getDescription(), indicator.getMeasureUnit(), indicator.getVariablesDefinition(),
                indicator.getDataFormula(), indicator.getDataSource(), indicator.getMethodologicalAspects(),
                indicator.getCalculationFrequency(), indicator.getMeasurementResponsible()));
    }

    private boolean hasName(Indicator indicator) { return indicator.getName() != null && !indicator.getName().isBlank(); }
    private String normalize(String text) { return text == null || text.isBlank() ? null : text.trim(); }
}
