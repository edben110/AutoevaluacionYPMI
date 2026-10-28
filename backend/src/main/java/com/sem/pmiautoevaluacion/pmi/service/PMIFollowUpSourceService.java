package com.sem.pmiautoevaluacion.pmi.service;

import com.sem.pmiautoevaluacion.pmi.dto.FollowUpSourceResponse;
import com.sem.pmiautoevaluacion.pmi.entity.*;
import com.sem.pmiautoevaluacion.pmi.repository.ObjectiveRepository;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PMIFollowUpSourceService {
    private final ObjectiveRepository objectives;
    public PMIFollowUpSourceService(ObjectiveRepository objectives) { this.objectives = objectives; }

    public Map<UUID, FollowUpSnapshot> rows(UUID planId) {
        Map<UUID, FollowUpSnapshot> rows = new LinkedHashMap<>();
        if (planId == null) return rows;
        for (Objective objective : objectives.findByPrioritaryComponent_Pmi_IdOrderByIdAsc(planId)) {
            var component = objective.getPrioritaryComponent().getValuation().getComponent();
            for (Goal goal : objective.getGoals().stream().sorted(Comparator.comparingInt(Goal::getPosition)).toList()) {
                for (Activity row : goal.getRows().stream().sorted(Comparator.comparingInt(Activity::getPosition)).toList()) {
                    if (!hasText(row.getIndicator().getName()) && !hasText(row.getDescription())) continue;
                    rows.put(row.getId(), new FollowUpSnapshot(component.getId(), objective.getId(), goal.getId(),
                            component.getProcess().getName(), component.getName(), objective.getDescription(), goal.getDescription(),
                            row.getIndicator().getName(), row.getDescription(), row.getStartDate(), row.getFinishDate()));
                }
            }
        }
        return rows;
    }

    public FollowUpSourceResponse response(FollowUpSnapshot s) {
        return new FollowUpSourceResponse(s.getComponentId(), s.getObjectiveId(), s.getGoalId(), s.getProcessName(),
                s.getComponentName(), s.getObjectiveDescription(), s.getGoalDescription(), s.getIndicatorName(),
                s.getActivities(), s.getStartDate(), s.getFinishDate());
    }

    public String fingerprint(Map<UUID, FollowUpSnapshot> rows) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            rows.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                var s = response(entry.getValue());
                for (Object value : Arrays.asList(entry.getKey(), s.componentId(), s.objectiveId(), s.goalId(), s.processName(),
                        s.componentName(), s.objectiveDescription(), s.goalDescription(), s.indicatorName(), s.activities(), s.startDate(), s.finishDate())) {
                    byte[] bytes = value == null ? null : value.toString().getBytes(StandardCharsets.UTF_8);
                    digest.update(ByteBuffer.allocate(4).putInt(bytes == null ? -1 : bytes.length).array());
                    if (bytes != null) digest.update(bytes);
                }
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
