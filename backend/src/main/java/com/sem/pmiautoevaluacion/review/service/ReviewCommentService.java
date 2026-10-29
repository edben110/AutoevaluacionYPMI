package com.sem.pmiautoevaluacion.review.service;

import com.sem.pmiautoevaluacion.review.dto.*;
import com.sem.pmiautoevaluacion.review.entity.ComponentReviewComment;
import com.sem.pmiautoevaluacion.review.repository.ComponentReviewCommentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.*;
import com.sem.pmiautoevaluacion.shared.exception.*;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewCommentService {
    private final DeliveryService deliveries;
    private final ComponentReviewCommentRepository comments;
    private final ComponentValuationRepository valuations;
    private final SelfEvaluationRepository evaluations;
    private final UserRepository users;
    private final Clock clock;
    public ReviewCommentService(DeliveryService deliveries, ComponentReviewCommentRepository comments,
            ComponentValuationRepository valuations, SelfEvaluationRepository evaluations,
            UserRepository users, Clock selfEvaluationClock) {
        this.deliveries = deliveries; this.comments = comments; this.valuations = valuations;
        this.evaluations = evaluations; this.users = users; this.clock = selfEvaluationClock;
    }

    @Transactional(readOnly = true)
    public List<ReviewCommentResponse> list(UUID institutionId, int year, UUID areaId) {
        var submission = deliveries.requireSubmission(institutionId, year, areaId);
        return comments.findBySubmission_IdOrderByCreatedAtAscIdAsc(submission.getId()).stream()
                .map(ReviewCommentResponse::from).toList();
    }

    @Transactional
    public ReviewCommentResponse add(UUID secretaryId, UUID institutionId, int year,
            UUID areaId, UUID componentId, ReviewCommentRequest request) {
        var user = users.findById(secretaryId).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!(user instanceof Secretary secretary)) throw new AccessDeniedException("Solo Secretaría puede registrar observaciones");
        if (request == null || request.body() == null || request.body().isBlank() || request.body().length() > 10000)
            throw new BadRequestException("Escribe una observación de hasta 10.000 caracteres");
        // Serializa con eliminación/republicación del período para no comentar una entrega reiniciada.
        evaluations.findForPmiUpdate(institutionId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación no encontrada"));
        var submission = deliveries.requireSubmission(institutionId, year, areaId);
        var valuation = valuations.findByEvaluation_IdAndComponent_Id(submission.getEvaluation().getId(), componentId)
                .filter(v -> v.getComponent().getProcess().getArea().getId().equals(areaId))
                .orElseThrow(() -> new ResourceNotFoundException("El componente no pertenece a esta entrega"));
        var comment = comments.save(new ComponentReviewComment(submission, valuation, secretary,
                request.body().trim(), clock.instant().truncatedTo(ChronoUnit.MICROS)));
        return ReviewCommentResponse.from(comment);
    }
}
