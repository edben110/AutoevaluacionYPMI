package com.sem.pmiautoevaluacion.selfEvaluation.service;

import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluationPeriod;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationPeriodRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import com.sem.pmiautoevaluacion.shared.value.PeriodRecord;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
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

    public SelfEvaluationPeriodService(SelfEvaluationPeriodRepository periods,
            SelfEvaluationRepository evaluations, UserRepository users,
            EstablishmentRepository establishments, Clock selfEvaluationClock) {
        this.periods = periods;
        this.evaluations = evaluations;
        this.users = users;
        this.establishments = establishments;
        this.clock = selfEvaluationClock;
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
        SelfEvaluationPeriod period = periods.findForConfiguration(year).orElseGet(() ->
                new SelfEvaluationPeriod(year, dates, secretary, clock.instant()));
        period.reschedule(dates, clock.instant());
        period = periods.save(period);
        // Vincula borradores anteriores sin alterar sus respuestas ni fechas de diligenciamiento.
        final SelfEvaluationPeriod configured = period;
        var existing = evaluations.findByYear(year);
        var institutionIds = new HashSet<UUID>();
        existing.forEach(evaluation -> {
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
        // Las respuestas y sus marcas de tiempo se conservan al retirar la habilitación.
        evaluations.findByYear(year).forEach(SelfEvaluation::removePeriod);
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
