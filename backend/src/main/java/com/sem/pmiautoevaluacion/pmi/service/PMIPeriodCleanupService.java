package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.pmi.repository.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Eliminación explícita de todos los anexos del PMI cuando Secretaría retira el período. */
@Service
public class PMIPeriodCleanupService {
    private final PMIRepository plans;
    private final PrioritaryComponentRepository priorities;
    private final ObjectiveRepository objectives;
    private final PMIFollowUpRepository followUps;
    private final PMIFollowUpCompletionRepository completions;

    public PMIPeriodCleanupService(PMIRepository plans, PrioritaryComponentRepository priorities,
            ObjectiveRepository objectives, PMIFollowUpRepository followUps, PMIFollowUpCompletionRepository completions) {
        this.plans = plans; this.priorities = priorities; this.objectives = objectives; this.followUps = followUps;
        this.completions = completions;
    }

    @Transactional
    public void deleteForEvaluation(UUID evaluationId) {
        // El llamador mantiene bloqueada la autoevaluación: ningún anexo puede guardar mientras se elimina.
        for (var plan : plans.findAllBySelfEvaluation_Id(evaluationId)) {
            completions.findByPmi_Id(plan.getId()).ifPresent(completions::delete);
            completions.flush();
            followUps.deleteAll(followUps.findByPmi_Id(plan.getId()));
            followUps.flush();
            // Las cascadas existentes eliminan metas, actividades e indicadores con sus fichas.
            objectives.deleteAll(objectives.findByPrioritaryComponent_Pmi_IdOrderByIdAsc(plan.getId()));
            objectives.flush();
            priorities.deleteAll(priorities.findByPmi_Id(plan.getId()));
            priorities.flush();
            plans.delete(plan);
        }
        plans.flush();
    }
}
