package com.sem.pmiautoevaluacion.selfEvaluation.service;

import com.sem.pmiautoevaluacion.integralManagement.entity.Component;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationSummary;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.ComponentValuation;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.ComponentValuationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfEvaluationService {
    private final SelfEvaluationRepository evaluations;
    private final ComponentValuationRepository valuations;
    private final EstablishmentRepository establishments;
    private final ComponentRepository components;

    public SelfEvaluationService(SelfEvaluationRepository evaluations,
            ComponentValuationRepository valuations,
            EstablishmentRepository establishments, ComponentRepository components) {
        this.evaluations = evaluations;
        this.valuations = valuations;
        this.establishments = establishments;
        this.components = components;
    }

    @Transactional(readOnly = true)
    public List<SelfEvaluationSummary> list(UUID establishmentId) {
        return evaluations.findByEstablishment_IdOrderByYearDesc(establishmentId).stream()
                .map(SelfEvaluationSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SelfEvaluationResponse find(UUID establishmentId, int year) {
        return response(findOwned(establishmentId, year));
    }

    @Transactional
    public SelfEvaluationResponse createOrFind(UUID establishmentId, int year) {
        validateYear(year);
        SelfEvaluation evaluation = evaluations.findByEstablishment_IdAndYear(establishmentId, year)
                .orElseGet(() -> {
                    Establishment establishment = establishments.findById(establishmentId)
                            .orElseThrow(() -> new ResourceNotFoundException("Institución no encontrada"));
                    return evaluations.save(new SelfEvaluation(establishment, year));
                });
        return response(evaluation);
    }

    @Transactional
    public ValuationResponse saveValuation(UUID establishmentId, int year, UUID componentId,
            ValuationRequest request) {
        SelfEvaluation evaluation = findOwned(establishmentId, year);
        Component component = components.findByIdAndState(componentId, UseState.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Componente activo no encontrado"));
        ComponentValuation valuation = valuations
                .findByEvaluation_IdAndComponent_Id(evaluation.getId(), componentId)
                .orElseGet(() -> new ComponentValuation(evaluation, component));
        valuation.update(request.level().shortValue(), normalizeUrl(request.evidenceUrl()),
                normalizeText(request.evidenceNote()));
        evaluation.touch();
        return ValuationResponse.from(valuations.save(valuation));
    }

    private SelfEvaluation findOwned(UUID establishmentId, int year) {
        validateYear(year);
        return evaluations.findByEstablishment_IdAndYear(establishmentId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Borrador de autoevaluación no encontrado"));
    }

    private SelfEvaluationResponse response(SelfEvaluation evaluation) {
        List<ValuationResponse> items = valuations.findByEvaluation_Id(evaluation.getId()).stream()
                .map(ValuationResponse::from).toList();
        return SelfEvaluationResponse.from(evaluation, items);
    }

    private void validateYear(int year) {
        if (year < 1) throw new BadRequestException("El año debe ser positivo");
    }

    private String normalizeUrl(String value) {
        String url = normalizeText(value);
        if (url == null) return null;
        try {
            URI uri = URI.create(url);
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null) return url;
        } catch (IllegalArgumentException ignored) {
            // La respuesta de validación se envía abajo.
        }
        throw new BadRequestException("El enlace de evidencia debe comenzar con http:// o https://");
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
