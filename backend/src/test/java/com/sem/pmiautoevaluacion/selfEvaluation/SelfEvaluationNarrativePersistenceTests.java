package com.sem.pmiautoevaluacion.selfEvaluation;

import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import com.sem.pmiautoevaluacion.users.entity.EmailRecord;
import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDate;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationPeriodService;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SelfEvaluationNarrativePersistenceTests {
    @Autowired SelfEvaluationService service;
    @Autowired EstablishmentRepository establishments;
    @Autowired EntityManager entityManager;
    @Autowired MockMvc mvc;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired UserRepository users;

    @BeforeEach
    void enablePeriodForNarrativeTests() {
        var secretary = users.save(new Secretary("Secretaría de prueba", "test-only",
                new EmailRecord(UUID.randomUUID() + "@local.test")));
        var today = LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), 2026, new SelfEvaluationPeriodRequest(today.minusDays(1), today.plusDays(1)));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void savesAndReloadsNarrativesAllowedBySelectedState(int level) throws Exception {
        String suffix = UUID.randomUUID().toString();
        Establishment institution = establishments.save(new Establishment("Institución de prueba", "test-only",
                new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
        UUID institutionId = institution.getId();
        UUID componentId = UUID.fromString("34002026-0003-4000-8000-000000000017");
        service.createOrFind(institutionId, 2026);

        service.saveValuation(institutionId, 2026, componentId, new ValuationRequest(level,
                "https://ejemplo.org/evidencia", " Evidencia institucional ",
                level == 1 ? null : "  Fortaleza institucional\nSegunda línea  ",
                level == 4 ? null : "  Oportunidad institucional  "));
        entityManager.flush();
        entityManager.clear();

        var saved = service.find(institutionId, 2026).valuations().getFirst();
        assertEquals(level, saved.level());
        assertEquals(level == 1 ? null : "Fortaleza institucional\nSegunda línea", saved.strengths());
        assertEquals(level == 4 ? null : "Oportunidad institucional", saved.improvementOpportunities());
        assertEquals("Evidencia institucional", saved.evidenceNote());
        assertEquals("https://ejemplo.org/evidencia", saved.evidenceUrl());
        mvc.perform(get("/api/self-evaluations/2026").with(user(new CustomUserDetails(institution))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valuations[0].strengthsAllowed").value(level != 1))
                .andExpect(jsonPath("$.valuations[0].improvementOpportunitiesAllowed").value(level != 4));

        // Corregir reemplaza el registro y permite borrar textos con campos vacíos.
        service.saveValuation(institutionId, 2026, componentId,
                new ValuationRequest(4, null, "Nota conservada", " \n ", null));
        entityManager.flush();
        entityManager.clear();
        var corrected = service.find(institutionId, 2026);
        assertEquals(1, corrected.valuations().size());
        assertEquals(1, corrected.totals().total());
        assertEquals(100.0, corrected.totals().percentages().continuousImprovement());
        assertNull(corrected.valuations().getFirst().strengths());
        assertNull(corrected.valuations().getFirst().improvementOpportunities());
        assertEquals("Nota conservada", corrected.valuations().getFirst().evidenceNote());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    void apiRejectsForbiddenNarrativesWithoutChangingSavedValuation(int selectedLevel) throws Exception {
        String suffix = UUID.randomUUID().toString();
        Establishment institution = establishments.save(new Establishment("Institución de prueba", "test-only",
                new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
        UUID institutionId = institution.getId();
        UUID componentId = UUID.fromString("34002026-0003-4000-8000-000000000017");
        service.createOrFind(institutionId, 2026);
        service.saveValuation(institutionId, 2026, componentId,
                new ValuationRequest(3, "https://ejemplo.org/original", "Nota original", "Fortaleza", "Mejora"));
        entityManager.flush();
        entityManager.clear();
        var original = service.find(institutionId, 2026).valuations().getFirst();

        mvc.perform(put("/api/self-evaluations/2026/valuations/" + componentId)
                .with(user(new CustomUserDetails(institution))).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"level":%d,"evidenceNote":"Cambio rechazado","strengths":"Fortaleza","improvementOpportunities":"Mejora"}
                        """.formatted(selectedLevel)))
                .andExpect(status().isBadRequest());
        entityManager.flush();
        entityManager.clear();
        assertEquals(original, service.find(institutionId, 2026).valuations().getFirst());
    }
}
