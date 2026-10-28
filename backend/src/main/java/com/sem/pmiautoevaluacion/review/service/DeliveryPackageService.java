package com.sem.pmiautoevaluacion.review.service;

import com.sem.pmiautoevaluacion.review.dto.*;
import com.sem.pmiautoevaluacion.review.entity.DeliveryPackageReview;
import com.sem.pmiautoevaluacion.review.repository.DeliveryPackageReviewRepository;
import com.sem.pmiautoevaluacion.integralManagement.repository.AreaRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.shared.exception.*;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryPackageService {
    private record Key(UUID institutionId, int year) {}
    private final DeliveryService deliveries;
    private final DeliveryPackageReviewRepository reviews;
    private final SelfEvaluationRepository evaluations;
    private final UserRepository users;
    private final AreaRepository areas;
    private final Clock clock;
    public DeliveryPackageService(DeliveryService deliveries, DeliveryPackageReviewRepository reviews,
            SelfEvaluationRepository evaluations, UserRepository users, AreaRepository areas, Clock selfEvaluationClock) {
        this.deliveries = deliveries; this.reviews = reviews; this.evaluations = evaluations;
        this.users = users; this.areas = areas; this.clock = selfEvaluationClock;
    }

    @Transactional(readOnly = true)
    public List<DeliveryPackageResponse> list(UUID institutionId) {
        var markers = institutionId == null ? reviews.findAllForPackages()
                : reviews.findByEvaluation_Establishment_Id(institutionId);
        var byEvaluation = markers.stream().collect(Collectors.toMap(
                r -> new Key(r.getEvaluation().getEstablishment().getId(), r.getEvaluation().getYear()), r -> r));
        var grouped = deliveries.list(institutionId).stream().collect(Collectors.groupingBy(
                d -> new Key(d.establishmentId(), d.year()), LinkedHashMap::new, Collectors.toList()));
        Set<UUID> activeAreas = areas.findAll().stream().filter(a -> a.getState() == UseState.ACTIVE)
                .map(a -> a.getId()).collect(Collectors.toSet());
        return grouped.entrySet().stream().map(entry -> {
            var row = entry.getValue().getFirst();
            var allAreas = new HashSet<>(activeAreas);
            entry.getValue().forEach(d -> allAreas.add(d.areaId()));
            var version = version(entry.getValue());
            var marker = byEvaluation.get(entry.getKey());
            boolean reviewed = marker != null && marker.getSourceVersion().equals(version);
            return new DeliveryPackageResponse(row.establishmentId(), row.establishmentName(), row.daneCode(),
                    row.year(), entry.getValue(), allAreas.size(), reviewed, marker != null && !reviewed,
                    marker == null ? null : marker.getReviewedAt(), marker == null ? null : marker.getAuthorName(), version);
        }).toList();
    }

    @Transactional
    public DeliveryPackageResponse mark(UUID secretaryId, UUID institutionId, int year, String expectedVersion) {
        var secretary = requireSecretary(secretaryId);
        var evaluation = lock(institutionId, year);
        var current = find(institutionId, year);
        if (!current.version().equals(expectedVersion))
            throw new BadRequestException("El paquete cambió desde que lo abriste. Actualiza las entregas y revisa los cambios antes de marcarlo");
        var marker = reviews.findByEvaluation_Id(evaluation.getId()).orElseGet(() -> new DeliveryPackageReview(evaluation));
        if (!current.reviewed()) {
            marker.mark(secretary, current.version(), clock.instant().truncatedTo(ChronoUnit.MICROS));
            reviews.saveAndFlush(marker);
        }
        return find(institutionId, year);
    }

    @Transactional
    public DeliveryPackageResponse pending(UUID secretaryId, UUID institutionId, int year) {
        requireSecretary(secretaryId);
        var evaluation = lock(institutionId, year);
        find(institutionId, year);
        reviews.findByEvaluation_Id(evaluation.getId()).ifPresent(reviews::delete);
        reviews.flush();
        return find(institutionId, year);
    }

    private DeliveryPackageResponse find(UUID institutionId, int year) {
        return list(institutionId).stream().filter(p -> p.year() == year).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No hay entregas recibidas para este paquete"));
    }
    private SelfEvaluation lock(UUID institutionId, int year) {
        if (year < 1 || year > 9999) throw new BadRequestException("El año debe estar entre 1 y 9999");
        return evaluations.findForPmiUpdate(institutionId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Autoevaluación no encontrada"));
    }
    private Secretary requireSecretary(UUID id) {
        var account = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!(account instanceof Secretary secretary)) throw new AccessDeniedException("Solo Secretaría puede marcar la revisión");
        return secretary;
    }
    private String version(List<DeliveryResponse> rows) {
        String source = rows.stream().sorted(Comparator.comparing(DeliveryResponse::areaId))
                .map(d -> d.areaId() + ":" + time(d.submittedAt()) + ":" + d.pmiId() + ":" + time(d.pmiUpdatedAt()))
                .collect(Collectors.joining("\n"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private String time(Instant instant) { return instant == null ? "" : instant.truncatedTo(ChronoUnit.MICROS).toString(); }
}
