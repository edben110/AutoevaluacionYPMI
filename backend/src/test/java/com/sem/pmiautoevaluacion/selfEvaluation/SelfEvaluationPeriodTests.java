package com.sem.pmiautoevaluacion.selfEvaluation;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationPeriodService;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import com.sem.pmiautoevaluacion.shared.value.PeriodRecord;
import com.sem.pmiautoevaluacion.users.entity.EmailRecord;
import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SelfEvaluationPeriodTests {
    private static final int YEAR = 8065;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);
    private static final UUID COMPONENT = UUID.fromString("34002026-0003-4000-8000-000000000017");
    @Autowired MockMvc mvc;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired SelfEvaluationService service;
    @Autowired SelfEvaluationRepository evaluations;
    @Autowired EstablishmentRepository establishments;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;
    @MockitoBean(name = "selfEvaluationClock") Clock clock;
    private Establishment institution;
    private Secretary secretary;

    @BeforeEach
    void fixtures() {
        when(clock.getZone()).thenReturn(ZoneId.of("America/Bogota"));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-06T17:00:00Z"));
        String suffix = UUID.randomUUID().toString();
        institution = establishments.save(new Establishment("Institución de prueba", "test-only",
                new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
        secretary = users.save(new Secretary("Secretaría de prueba", "test-only",
                new EmailRecord(UUID.randomUUID() + "@local.test")));
    }

    @Test
    void publicationCreatesDraftsForAllInstitutionsAndReschedulingPreservesAnswers() throws Exception {
        // Simula un borrador creado antes de incorporar los períodos.
        var legacy = evaluations.save(new SelfEvaluation(institution, YEAR));
        UUID originalId = legacy.getId();
        mvc.perform(put("/api/self-evaluation-periods/" + YEAR)
                .with(user(new CustomUserDetails(secretary))).contentType(MediaType.APPLICATION_JSON)
                .content(body(TODAY, TODAY.plusDays(4))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.writable").value(true));
        assertEquals(establishments.count(), evaluations.findByYear(YEAR).size());
        assertEquals(YEAR, legacy.getPeriod().getYear());
        service.saveValuation(institution.getId(), YEAR, COMPONENT,
                new ValuationRequest(2, null, "Evidencia", "Fortaleza", "Mejora"));
        entityManager.flush();
        entityManager.clear();
        var saved = service.find(institution.getId(), YEAR);
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(TODAY.plusDays(5), TODAY.plusDays(8)));
        entityManager.flush();
        entityManager.clear();
        assertEquals(saved, service.find(institution.getId(), YEAR));
        assertEquals(originalId, service.find(institution.getId(), YEAR).id());
        assertEquals(establishments.count(), evaluations.findByYear(YEAR).size());
        assertFalse(periods.find(YEAR).writable());
        mvc.perform(get("/api/self-evaluations/" + YEAR).with(user(new CustomUserDetails(institution))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valuations[0].strengths").value("Fortaleza"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void boundariesIncludeBothDatesAndSingleDayWindows(int dayOffset) throws Exception {
        LocalDate start = TODAY.plusDays(dayOffset == 1 ? -3 : dayOffset);
        LocalDate finish = dayOffset == -1 ? TODAY.plusDays(3) : TODAY;
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(start, finish));
        mvc.perform(put("/api/self-evaluations/" + YEAR).with(user(new CustomUserDetails(institution))))
                .andExpect(status().isOk());
        mvc.perform(put("/api/self-evaluations/" + YEAR + "/valuations/" + COMPONENT)
                .with(user(new CustomUserDetails(institution))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":1,\"improvementOpportunities\":\"Mejora\"}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SCHEDULED", "CLOSED"})
    void outsideWindowRejectsWritesButAllowsConsultationWithoutChangingAnswers(String state) throws Exception {
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(TODAY, TODAY));
        service.saveValuation(institution.getId(), YEAR, COMPONENT,
                new ValuationRequest(3, null, "Original", "Fortaleza", "Mejora"));
        var original = service.find(institution.getId(), YEAR);
        var boundary = "SCHEDULED".equals(state) ? TODAY.plusDays(2) : TODAY.minusDays(2);
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(boundary, boundary));
        assertEquals(state, periods.find(YEAR).status());
        mvc.perform(put("/api/self-evaluations/" + YEAR).with(user(new CustomUserDetails(institution))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/self-evaluations/" + YEAR + "/valuations/" + COMPONENT)
                .with(user(new CustomUserDetails(institution))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":1,\"evidenceNote\":\"Rechazado\"}"))
                .andExpect(status().isForbidden());
        assertEquals(original, service.find(institution.getId(), YEAR));
        mvc.perform(get("/api/self-evaluations/" + YEAR).with(user(new CustomUserDetails(secretary)))
                .param("establishmentId", institution.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void missingPeriodPreventsInstitutionWritesAndLeavesLegacyDraftReadable() throws Exception {
        evaluations.save(new SelfEvaluation(institution, YEAR));
        mvc.perform(get("/api/self-evaluation-periods/" + YEAR).with(user(new CustomUserDetails(institution))))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/self-evaluations/" + YEAR).with(user(new CustomUserDetails(institution))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Secretaría aún no ha habilitado la autoevaluación de " + YEAR));
        mvc.perform(put("/api/self-evaluations/" + YEAR + "/valuations/" + COMPONENT)
                .with(user(new CustomUserDetails(institution))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":2}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/self-evaluations/" + YEAR).with(user(new CustomUserDetails(institution))))
                .andExpect(status().isOk());
    }

    @Test
    void institutionCannotConfigureDatesAndAnonymousCannotAccessPeriods() throws Exception {
        mvc.perform(put("/api/self-evaluation-periods/" + YEAR).with(user(new CustomUserDetails(institution)))
                .contentType(MediaType.APPLICATION_JSON).content(body(TODAY, TODAY)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/self-evaluation-periods"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/self-evaluation-periods/" + YEAR)
                .contentType(MediaType.APPLICATION_JSON).content(body(TODAY, TODAY)))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"startDate\":\"2026-10-08\",\"finishDate\":\"2026-10-06\"}",
            "{\"startDate\":\"2026-10-06\"}",
            "{\"startDate\":\"2026-02-30\",\"finishDate\":\"2026-10-06\"}"})
    void rejectsInvalidDatesWithoutPublishingPeriodOrDrafts(String body) throws Exception {
        mvc.perform(put("/api/self-evaluation-periods/" + YEAR)
                .with(user(new CustomUserDetails(secretary))).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertTrue(evaluations.findByYear(YEAR).isEmpty());
        mvc.perform(get("/api/self-evaluation-periods/" + YEAR).with(user(new CustomUserDetails(secretary))))
                .andExpect(status().isNotFound());
    }

    @Test
    void periodUsesBogotaDateInsteadOfUtcAndInstitutionCanReadDates() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-07T04:59:59Z"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(TODAY, TODAY));
        mvc.perform(get("/api/self-evaluation-periods/" + YEAR).with(user(new CustomUserDetails(institution))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.today").value("2026-10-06"))
                .andExpect(jsonPath("$.writable").value(true)).andExpect(jsonPath("$.timeZone").value("America/Bogota"));
        mvc.perform(get("/api/self-evaluation-periods").with(user(new CustomUserDetails(institution))))
                .andExpect(status().isOk());
        when(clock.instant()).thenReturn(Instant.parse("2026-10-07T05:00:00Z"));
        assertEquals("CLOSED", periods.find(YEAR).status());
        assertFalse(periods.find(YEAR).writable());
    }

    @Test
    void newInstitutionCanStartDraftInAlreadyPublishedOpenPeriod() {
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(TODAY, TODAY));
        String suffix = UUID.randomUUID().toString();
        var late = establishments.save(new Establishment("Nueva institución", "test-only",
                new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
        assertTrue(evaluations.findByEstablishment_IdAndYear(late.getId(), YEAR).isEmpty());
        var draft = service.createOrFind(late.getId(), YEAR);
        assertEquals(YEAR, draft.year());
        assertEquals(YEAR, evaluations.findByEstablishment_IdAndYear(late.getId(), YEAR).orElseThrow().getPeriod().getYear());
    }

    @Test
    void periodRecordIncludesEdgesAndCountsCalendarDays() {
        var dates = new PeriodRecord(TODAY, TODAY.plusDays(2));
        assertTrue(dates.include(TODAY));
        assertTrue(dates.include(TODAY.plusDays(2)));
        assertFalse(dates.include(TODAY.minusDays(1)));
        assertFalse(dates.include(null));
        assertEquals(3, dates.daysDuration());
    }

    @Test
    void secretaryDeletesWindowPreservingAnswersAndCanReopenSameYear() throws Exception {
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(TODAY, TODAY));
        service.saveValuation(institution.getId(), YEAR, COMPONENT,
                new ValuationRequest(2, "https://ejemplo.org/evidencia", "Evidencia", "Fortaleza", "Mejora"));
        entityManager.flush();
        entityManager.clear();
        var original = service.find(institution.getId(), YEAR);
        var originalCount = evaluations.findByYear(YEAR).size();
        mvc.perform(delete("/api/self-evaluation-periods/" + YEAR).with(user(new CustomUserDetails(secretary))))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        entityManager.flush();
        entityManager.clear();
        assertEquals(originalCount, evaluations.findByYear(YEAR).size());
        assertEquals(original, service.find(institution.getId(), YEAR));
        assertTrue(evaluations.findByYear(YEAR).stream().allMatch(evaluation -> evaluation.getPeriod() == null));
        mvc.perform(get("/api/self-evaluation-periods/" + YEAR).with(user(new CustomUserDetails(secretary))))
                .andExpect(status().isNotFound());
        assertTrue(periods.list().stream().noneMatch(period -> period.year() == YEAR));
        mvc.perform(get("/api/self-evaluations/" + YEAR).with(user(new CustomUserDetails(institution))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valuations[0].strengths").value("Fortaleza"));
        mvc.perform(put("/api/self-evaluations/" + YEAR + "/valuations/" + COMPONENT)
                .with(user(new CustomUserDetails(institution))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":1}"))
                .andExpect(status().isForbidden());
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(TODAY, TODAY.plusDays(2)));
        entityManager.flush();
        entityManager.clear();
        assertEquals(original, service.find(institution.getId(), YEAR));
        assertEquals(originalCount, evaluations.findByYear(YEAR).size());
        assertTrue(evaluations.findByYear(YEAR).stream().allMatch(evaluation -> evaluation.getPeriod().getYear() == YEAR));
    }

    @Test
    void institutionAndAnonymousCannotDeleteWindow() throws Exception {
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(TODAY, TODAY));
        var original = periods.find(YEAR);
        mvc.perform(delete("/api/self-evaluation-periods/" + YEAR).with(user(new CustomUserDetails(institution))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/self-evaluation-periods/" + YEAR))
                .andExpect(status().isUnauthorized());
        assertEquals(original, periods.find(YEAR));
        assertNotNull(evaluations.findByEstablishment_IdAndYear(institution.getId(), YEAR).orElseThrow().getPeriod());
    }

    @Test
    void missingOrInvalidWindowCannotBeDeletedAndOtherYearsStayIntact() throws Exception {
        periods.configure(secretary.getId(), YEAR + 1, new SelfEvaluationPeriodRequest(TODAY, TODAY));
        var other = periods.find(YEAR + 1);
        mvc.perform(delete("/api/self-evaluation-periods/" + YEAR).with(user(new CustomUserDetails(secretary))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/self-evaluation-periods/0").with(user(new CustomUserDetails(secretary))))
                .andExpect(status().isBadRequest());
        assertEquals(other, periods.find(YEAR + 1));
        assertNotNull(evaluations.findByEstablishment_IdAndYear(institution.getId(), YEAR + 1).orElseThrow().getPeriod());
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, 2})
    void secretaryCanDeleteClosedOrScheduledWindow(int offset) throws Exception {
        var date = TODAY.plusDays(offset);
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(date, date));
        mvc.perform(delete("/api/self-evaluation-periods/" + YEAR).with(user(new CustomUserDetails(secretary))))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();
        assertTrue(periods.list().stream().noneMatch(period -> period.year() == YEAR));
        assertNotNull(service.find(institution.getId(), YEAR));
    }

    private String body(LocalDate start, LocalDate finish) {
        return "{\"startDate\":\"" + start + "\",\"finishDate\":\"" + finish + "\"}";
    }
}
