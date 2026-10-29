package com.sem.pmiautoevaluacion.integralManagement.service;

import com.sem.pmiautoevaluacion.integralManagement.dto.ComponentGuidanceResponse;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ComponentGuidanceService {
    private static final String DEFAULT_EVIDENCE_NOTICE =
            "Los ejemplos de evidencia son orientativos, no obligatorios ni exhaustivos.";
    private final ComponentRepository components;
    private final GuidanceCatalog catalog;
    private final Map<UUID, GuidanceItem> officialItems;

    public ComponentGuidanceService(ComponentRepository components, ObjectMapper mapper) {
        this.components = components;
        try (var input = new ClassPathResource("guidance/guia34-2026.json").getInputStream()) {
            this.catalog = mapper.readValue(input, GuidanceCatalog.class);
            this.officialItems = catalog.components().stream()
                    .collect(Collectors.toUnmodifiableMap(GuidanceItem::componentId, Function.identity()));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("No se pudo cargar la orientación de componentes de la Guía 34", exception);
        }
    }

    @Transactional(readOnly = true)
    public ComponentGuidanceResponse find(UUID componentId) {
        var component = components.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Componente no encontrado"));
        var process = component.getProcess();
        var area = process.getArea();
        GuidanceItem item = officialItems.get(componentId);
        boolean official = item != null && process.getId().equals(item.processId())
                && component.getName().equals(item.componentName());
        String notice = normalize(catalog.evidenceNotice());
        if (notice == null) notice = DEFAULT_EVIDENCE_NOTICE;
        if (official) {
            return new ComponentGuidanceResponse(componentId, component.getName(), area.getName(), process.getName(),
                    normalize(item.evaluationItem()), cleanExamples(item.evidenceExamples()), notice,
                    normalize(catalog.sourceTitle()), normalize(catalog.sourceUrl()),
                    pages(item.itemPages()), pages(item.evidencePages()), true);
        }
        String expectedEvidence = normalize(component.getExpectedEvidence());
        return new ComponentGuidanceResponse(componentId, component.getName(), area.getName(), process.getName(),
                normalize(component.getDescription()), expectedEvidence == null ? List.of() : List.of(expectedEvidence),
                notice, null, null, List.of(), List.of(), false);
    }

    private List<String> cleanExamples(List<String> examples) {
        return examples == null ? List.of() : examples.stream().map(this::normalize)
                .filter(example -> example != null).toList();
    }

    private List<Integer> pages(List<Integer> values) { return values == null ? List.of() : List.copyOf(values); }
    private String normalize(String text) { return text == null || text.isBlank() ? null : text.trim(); }

    private record GuidanceCatalog(String sourceTitle, String sourceUrl, String evidenceNotice,
            List<GuidanceItem> components) {}
    private record GuidanceItem(UUID componentId, UUID processId, String componentName,
            String evaluationItem, List<String> evidenceExamples, List<Integer> itemPages,
            List<Integer> evidencePages) {}
}
