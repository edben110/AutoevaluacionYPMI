package com.sem.pmiautoevaluacion.review.service;

import com.sem.pmiautoevaluacion.review.dto.*;
import com.sem.pmiautoevaluacion.pmi.repository.PMIRepository;
import com.sem.pmiautoevaluacion.pmi.entity.PMI;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluationAreaSubmission;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.*;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DeliveryService {
    private final SelfEvaluationAreaSubmissionRepository submissions;
    private final SelfEvaluationRepository evaluations;
    private final ComponentValuationRepository valuations;
    private final PMIRepository plans;

    public DeliveryService(SelfEvaluationAreaSubmissionRepository submissions, SelfEvaluationRepository evaluations,
            ComponentValuationRepository valuations, PMIRepository plans) {
        this.submissions = submissions; this.evaluations = evaluations;
        this.valuations = valuations; this.plans = plans;
    }

    public List<DeliveryResponse> list(UUID institutionId) {
        var rows = institutionId == null ? submissions.findAllForReview()
                : submissions.findByEvaluation_Establishment_Id(institutionId);
        if (rows.isEmpty()) return List.of();
        var evaluationIds = rows.stream().map(s -> s.getEvaluation().getId()).distinct().toList();
        var byEvaluation = plans.findBySelfEvaluation_IdIn(evaluationIds).stream().collect(
                java.util.stream.Collectors.groupingBy(p -> p.getSelfEvaluation().getId(),
                        java.util.stream.Collectors.toMap(p -> p.getArea().getId(), p -> p)));
        return rows.stream().map(s -> summary(s, byEvaluation.getOrDefault(s.getEvaluation().getId(), Map.of()).get(s.getArea().getId())))
                .sorted(Comparator.comparingInt(DeliveryResponse::year).reversed()
                        .thenComparing(DeliveryResponse::establishmentName)
                        .thenComparing(DeliveryResponse::areaName)).toList();
    }

    public DeliveryDetailResponse find(UUID institutionId, int year, UUID areaId) {
        var submission = requireSubmission(institutionId, year, areaId);
        // Incluye los componentes valorados aunque después se desactiven en el catálogo.
        var components = valuations.findByEvaluation_Id(submission.getEvaluation().getId()).stream()
                .filter(v -> v.getComponent().getProcess().getArea().getId().equals(areaId))
                .sorted(Comparator.comparing((com.sem.pmiautoevaluacion.selfEvaluation.entity.ComponentValuation v)
                        -> v.getComponent().getProcess().getId()).thenComparing(v -> v.getComponent().getId()))
                .map(v -> new ReviewedComponentResponse(v.getComponent().getId(), v.getComponent().getName(),
                        v.getComponent().getDescription(), v.getComponent().getProcess().getId(),
                        v.getComponent().getProcess().getName(), ValuationResponse.from(v))).toList();
        return new DeliveryDetailResponse(summary(submission), components);
    }

    public SelfEvaluationAreaSubmission requireSubmission(UUID institutionId, int year, UUID areaId) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
        var evaluation = evaluations.findByEstablishment_IdAndYear(institutionId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación no encontrada"));
        return submissions.findByEvaluation_IdAndArea_Id(evaluation.getId(), areaId)
                .orElseThrow(() -> new ResourceNotFoundException("El área aún no tiene una autoevaluación enviada"));
    }

    private DeliveryResponse summary(SelfEvaluationAreaSubmission submission) {
        return summary(submission, plans.findBySelfEvaluation_IdAndArea_Id(
                submission.getEvaluation().getId(), submission.getArea().getId()).orElse(null));
    }

    private DeliveryResponse summary(SelfEvaluationAreaSubmission submission, PMI plan) {
        var evaluation = submission.getEvaluation();
        var institution = evaluation.getEstablishment();
        return new DeliveryResponse(institution.getId(), institution.getName(), institution.getDaneCode(),
                evaluation.getYear(), submission.getArea().getId(), submission.getArea().getName(),
                submission.getSubmittedAt(), plan == null ? null : plan.getId(), plan == null ? null : plan.getUpdatedAt());
    }
}
