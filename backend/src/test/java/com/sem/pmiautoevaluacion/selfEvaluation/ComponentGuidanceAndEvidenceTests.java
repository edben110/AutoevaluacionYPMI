package com.sem.pmiautoevaluacion.selfEvaluation;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.entity.Component;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.integralManagement.repository.ProcessRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationPeriodService;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import com.sem.pmiautoevaluacion.users.entity.*;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComponentGuidanceAndEvidenceTests {
    private static final int YEAR = 8074;
    private static final UUID A = component(17);
    private static final String SOURCE = "https://www.mineducacion.gov.co/1780/articles-430002_recurso_3.pdf";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ComponentRepository components;
    @Autowired ProcessRepository processes;
    @Autowired EstablishmentRepository establishments;
    @Autowired UserRepository users;
    @Autowired SelfEvaluationService evaluations;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired EntityManager em;
    private Establishment owner;
    private Secretary secretary;

    @BeforeEach
    void fixtures() {
        String suffix = UUID.randomUUID().toString();
        owner = establishments.save(new Establishment("Institución de prueba", "test-only", new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
        secretary = users.save(new Secretary("Secretaría de prueba", "test-only", new EmailRecord(UUID.randomUUID() + "@local.test")));
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
    }

    static Stream<String> references() {
        return Stream.of(
                "Archivo físico: acta 12 de 2026, folios 30 a 70. Consultar en coordinación académica.",
                "https://example.com/carpeta\nConsultar también el libro físico de actas, tomo 2.",
                "Descripción de evidencia documental. ".repeat(70),
                "Texto literal <documento> y referencia interna; no es un enlace web.");
    }

    @ParameterizedTest
    @MethodSource("references")
    void evidenceReferenceAcceptsTextLinksAndLongMultilineDescriptions(String reference) throws Exception {
        // Un enlace anterior se conserva hasta que la institución lo sustituye explícitamente.
        evaluations.saveValuation(owner.getId(), YEAR, A, new ValuationRequest(2, "https://example.com/anterior", "Nota conservada", null, null));
        mvc.perform(put(valuationPath()).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("level", 2, "evidenceUrl", "  " + reference + "  ", "evidenceNote", "Nota conservada"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.evidenceUrl").value(reference.trim()));
        em.flush(); em.clear();
        var saved = evaluations.find(owner.getId(), YEAR).valuations().getFirst();
        assertEquals(reference.trim(), saved.evidenceUrl());
        assertEquals("Nota conservada", saved.evidenceNote());
    }

    @Test
    void evidenceLengthLimitRejectsWithoutReplacingExistingReference() throws Exception {
        evaluations.saveValuation(owner.getId(), YEAR, A, new ValuationRequest(2, "Archivo físico original", null, null, null));
        mvc.perform(put(valuationPath()).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("level", 2, "evidenceUrl", "x".repeat(20003)))))
                .andExpect(status().isBadRequest());
        assertEquals("Archivo físico original", evaluations.find(owner.getId(), YEAR).valuations().getFirst().evidenceUrl());
    }

    @Test
    void unifiedEvidencePreservesBothLegacyFieldsAtTheirMaximumLengthWithoutDuplicatingTheNote() throws Exception {
        String reference = "a".repeat(10000);
        String note = "b".repeat(10000);
        evaluations.saveValuation(owner.getId(), YEAR, A, new ValuationRequest(2, reference, note, null, null));
        em.flush(); em.clear();
        var legacy = evaluations.find(owner.getId(), YEAR).valuations().getFirst();
        assertEquals(reference, legacy.evidenceUrl());
        assertEquals(note, legacy.evidenceNote());

        String combined = legacy.evidenceUrl() + "\n\n" + legacy.evidenceNote();
        mvc.perform(put(valuationPath()).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("level", 2, "evidenceUrl", combined))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.evidenceUrl").value(combined));
        em.flush(); em.clear();
        var saved = evaluations.find(owner.getId(), YEAR).valuations().getFirst();
        assertEquals(combined, saved.evidenceUrl());
        assertNull(saved.evidenceNote());
    }

    @Test
    void everyOfficialComponentHasItsOwnItemAndTraceableEvidenceExamples() throws Exception {
        for (int number = 1; number <= 45; number++) {
            mvc.perform(get(guidancePath(component(number))).with(user(principal(owner))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.componentId").value(component(number).toString()))
                    .andExpect(jsonPath("$.officialGuidance").value(true))
                    .andExpect(jsonPath("$.evaluationItem", not(blankOrNullString())))
                    .andExpect(jsonPath("$.evidenceExamples", not(empty())))
                    .andExpect(jsonPath("$.itemPages", not(empty())))
                    .andExpect(jsonPath("$.evidencePages", not(empty())))
                    .andExpect(jsonPath("$.sourceUrl").value(SOURCE));
        }
    }

    @Test
    void financialHelpIsScopedToItsComponentWithItsActualGuidePages() throws Exception {
        mvc.perform(get(guidancePath(component(36))).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itemPages", hasItem(153)))
                .andExpect(jsonPath("$.evidencePages", hasItem(174)))
                .andExpect(jsonPath("$.componentName", containsString("Fondo de Servicios Educativos")))
                .andExpect(jsonPath("$.evidenceNotice", not(blankOrNullString())));
        mvc.perform(get(guidancePath(component(28))).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itemPages", hasItem(145)))
                .andExpect(jsonPath("$.componentName", containsString("matrícula")));
    }

    @Test
    void customComponentUsesItsOwnCatalogHelpWithoutClaimingAnOfficialSource() throws Exception {
        Component custom = new Component("Componente de prueba " + UUID.randomUUID(), "Descripción propia del catálogo", "Actas pertinentes al componente de prueba");
        custom.setProcess(components.findById(A).orElseThrow().getProcess());
        components.saveAndFlush(custom);
        mvc.perform(get(guidancePath(custom.getId())).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.officialGuidance").value(false))
                .andExpect(jsonPath("$.evaluationItem").value("Descripción propia del catálogo"))
                .andExpect(jsonPath("$.evidenceExamples", hasItem("Actas pertinentes al componente de prueba")))
                .andExpect(jsonPath("$.sourceUrl").value(nullValue()))
                .andExpect(jsonPath("$.itemPages", empty()));
    }

    @Test
    void renamingOrMovingAComponentDoesNotShowAnUnrelatedOfficialItem() throws Exception {
        Component renamed = components.findById(A).orElseThrow();
        renamed.setName("Componente con alcance modificado");
        renamed.setDescription("Alcance modificado del catálogo");
        components.saveAndFlush(renamed);
        mvc.perform(get(guidancePath(A)).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.officialGuidance").value(false))
                .andExpect(jsonPath("$.evaluationItem").value("Alcance modificado del catálogo"));
        Component moved = components.findById(component(18)).orElseThrow();
        moved.setProcess(processes.findById(UUID.fromString("34002026-0002-4000-8000-000000000009")).orElseThrow());
        components.saveAndFlush(moved);
        mvc.perform(get(guidancePath(moved.getId())).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.officialGuidance").value(false))
                .andExpect(jsonPath("$.evidencePages", empty()));
    }

    @Test
    void guidanceIsReadOnlyForBothRolesAndRequiresAuthentication() throws Exception {
        mvc.perform(get(guidancePath(A)).with(user(principal(secretary)))).andExpect(status().isOk());
        mvc.perform(get(guidancePath(A))).andExpect(status().isUnauthorized());
        mvc.perform(get(guidancePath(UUID.randomUUID())).with(user(principal(owner)))).andExpect(status().isNotFound());
    }

    private String valuationPath() { return "/api/self-evaluations/" + YEAR + "/valuations/" + A; }
    private String guidancePath(UUID id) { return "/api/components/" + id + "/guidance"; }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
    private static UUID component(int id) { return UUID.fromString("34002026-0003-4000-8000-%012d".formatted(id)); }
}
