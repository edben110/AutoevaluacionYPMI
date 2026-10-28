package com.sem.pmiautoevaluacion.selfEvaluation.service;

import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluationPeriod;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationPeriodRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.ComponentValuationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationAreaSubmissionRepository;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import com.sem.pmiautoevaluacion.shared.value.PeriodRecord;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.pmi.service.PMIPeriodCleanupService;
import com.sem.pmiautoevaluacion.review.repository.ComponentReviewCommentRepository;
import com.sem.pmiautoevaluacion.review.repository.DeliveryPackageReviewRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfEvaluationPeriodService {
    private final SelfEvaluationPeriodRepository periods;
    private final SelfEvaluationRepository evaluations;
    private final UserRepository users;
    private final EstablishmentRepository establishments;
    private final Clock clock;
    private final PMIPeriodCleanupService pmiCleanup;
    private final ComponentValuationRepository valuations;
    private final SelfEvaluationAreaSubmissionRepository submissions;
    private final ComponentReviewCommentRepository reviewComments;
    private final DeliveryPackageReviewRepository packageReviews;

    public SelfEvaluationPeriodService(SelfEvaluationPeriodRepository periods,
            SelfEvaluationRepository evaluations, UserRepository users,
            EstablishmentRepository establishments, Clock selfEvaluationClock, PMIPeriodCleanupService pmiCleanup,
            ComponentValuationRepository valuations, SelfEvaluationAreaSubmissionRepository submissions,
            ComponentReviewCommentRepository reviewComments, DeliveryPackageReviewRepository packageReviews) {
        this.periods = periods;
        this.evaluations = evaluations;
        this.users = users;
        this.establishments = establishments;
        this.clock = selfEvaluationClock;
        this.pmiCleanup = pmiCleanup;
        this.valuations = valuations;
        this.submissions = submissions;
        this.reviewComments = reviewComments;
        this.packageReviews = packageReviews;
    }

    @Transactional(readOnly = true)
    public List<SelfEvaluationPeriodResponse> list() {
        return periods.findAllByOrderByYearDesc().stream()
                .map(period -> SelfEvaluationPeriodResponse.from(period, clock)).toList();
    }

    @Transactional(readOnly = true)
    public SelfEvaluationPeriodResponse find(int year) {
        validateYear(year);
        return SelfEvaluationPeriodResponse.from(periods.findById(year).orElseThrow(() ->
                new ResourceNotFoundException("Secretaría aún no ha configurado el período de " + year)), clock);
    }

    @Transactional
    public SelfEvaluationPeriodResponse configure(UUID secretaryId, int year, SelfEvaluationPeriodRequest request) {
        validateYear(year);
        PeriodRecord dates = new PeriodRecord(request.startDate(), request.finishDate());
        Secretary secretary = requireSecretary(secretaryId);
        var previousPeriod = periods.findForConfiguration(year);
        boolean publishingNewPeriod = previousPeriod.isEmpty();
        SelfEvaluationPeriod period = previousPeriod.orElseGet(() ->
                new SelfEvaluationPeriod(year, dates, secretary, clock.instant()));
        period.reschedule(dates, clock.instant());
        period = periods.save(period);
        // Publicar un período nuevo prepara borradores vacíos. Editar fechas conserva las respuestas.
        final SelfEvaluationPeriod configured = period;
        var existing = evaluations.findByYearForPmiDeletion(year);
        var institutionIds = new HashSet<UUID>();
        existing.forEach(evaluation -> {
            if (publishingNewPeriod) {
                // También limpia registros de versiones anteriores que quedaron sin período.
                pmiCleanup.deleteForEvaluation(evaluation.getId());
                packageReviews.findByEvaluation_Id(evaluation.getId()).ifPresent(packageReviews::delete);
                packageReviews.flush();
                reviewComments.deleteAll(reviewComments.findBySubmission_Evaluation_Id(evaluation.getId()));
                reviewComments.flush();
                submissions.deleteAll(submissions.findByEvaluation_Id(evaluation.getId()));
                submissions.flush();
                valuations.deleteAll(valuations.findByEvaluation_Id(evaluation.getId()));
                valuations.flush();
                evaluation.restartDraft(clock.instant());
            }
            evaluation.assignPeriod(configured);
            institutionIds.add(evaluation.getEstablishment().getId());
        });
        establishments.findAll().stream()
                .filter(institution -> !institutionIds.contains(institution.getId()))
                .forEach(institution -> {
                    var draft = new SelfEvaluation(institution, year);
                    draft.assignPeriod(configured);
                    evaluations.save(draft);
                });
        return SelfEvaluationPeriodResponse.from(period, clock);
    }

    @Transactional
    public void delete(UUID secretaryId, int year) {
        validateYear(year);
        requireSecretary(secretaryId);
        SelfEvaluationPeriod period = periods.findForConfiguration(year).orElseThrow(() ->
                new ResourceNotFoundException("El período de autoevaluación de " + year + " no existe"));
        // Período → autoevaluaciones: mismo orden de bloqueo que el diligenciamiento y envío.
        // Se conservan respuestas/envíos, pero se eliminan todos los PMI del año y sus anexos.
        evaluations.findByYearForPmiDeletion(year).forEach(evaluation -> {
            pmiCleanup.deleteForEvaluation(evaluation.getId());
            evaluation.removePeriod();
        });
        evaluations.flush();
        periods.delete(period);
    }

    @Transactional
    public SelfEvaluationPeriod requireWritable(int year) {
        validateYear(year);
        SelfEvaluationPeriod period = periods.findForDiligence(year).orElseThrow(() ->
                new SelfEvaluationPeriodClosedException("Secretaría aún no ha habilitado la autoevaluación de " + year));
        LocalDate today = LocalDate.now(clock);
        if (!period.getPeriod().include(today)) {
            throw new SelfEvaluationPeriodClosedException(today.isBefore(period.getPeriod().getStartDate())
                    ? "El período de autoevaluación todavía no ha iniciado"
                    : "El período de autoevaluación terminó; solo puedes consultar tus respuestas");
        }
        return period;
    }

    private Secretary requireSecretary(UUID secretaryId) {
        var user = users.findById(secretaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario de Secretaría no encontrado"));
        if (!(user instanceof Secretary secretary)) {
            throw new AccessDeniedException("Solo Secretaría puede administrar los períodos");
        }
        return secretary;
    }

    private void validateYear(int year) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
    }
}
