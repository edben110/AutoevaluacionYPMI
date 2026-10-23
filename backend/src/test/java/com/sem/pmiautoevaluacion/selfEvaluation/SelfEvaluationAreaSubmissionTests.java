package com.sem.pmiautoevaluacion.selfEvaluation;

import com.jayway.jsonpath.JsonPath;
import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationPeriodService;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.users.entity.*;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SelfEvaluationAreaSubmissionTests {
    private static final int YEAR = 8075;
    private static final UUID AREA = UUID.fromString("34002026-0001-4000-8000-000000000002");
    private static final UUID OTHER_AREA = UUID.fromString("34002026-0001-4000-8000-000000000001");
    private static final UUID A = component(17);
    @Autowired MockMvc mvc;
    @Autowired ComponentRepository catalog;
    @Autowired EstablishmentRepository institutions;
    @Autowired UserRepository users;
    @Autowired SelfEvaluationRepository evaluations;
    @Autowired SelfEvaluationService selfEvaluations;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    private Establishment owner, other;
    private Secretary secretary;
    private LocalDate today;

    @BeforeEach
    void fixtures() {
        owner = institution(); other = institution();
        secretary = users.save(new Secretary("Secretaría de prueba", "test-only", new EmailRecord(UUID.randomUUID() + "@local.test")));
        today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
    }

    @Test
    void completingAnswersDoesNotSubmitOrEnablePmiAutomatically() throws Exception {
        fill(AREA, 1);
        mvc.perform(get(areaPath()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.complete").value(true)).andExpect(jsonPath("$.canSubmit").value(true))
                .andExpect(jsonPath("$.submittedAt").value(nullValue()));
        mvc.perform(get(pmiPath()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.selfEvaluationComplete").value(true))
                .andExpect(jsonPath("$.selfEvaluationSubmitted").value(false))
                .andExpect(jsonPath("$.canFormulate").value(false));
        mvc.perform(put(pmiPath() + "/components/" + A).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"criticalFactors\":\"Factores\",\"urgency\":5,\"tendency\":5,\"impact\":5}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, submissionCount());
    }

    @Test
    void cannotSubmitAnIncompleteOrEmptyArea() throws Exception {
        mvc.perform(post(areaPath() + "/submit").with(user(principal(owner))))
                .andExpect(status().isBadRequest());
        selfEvaluations.saveValuation(owner.getId(), YEAR, A, new ValuationRequest(1, null, null, null, null));
        mvc.perform(post(areaPath() + "/submit").with(user(principal(owner))))
                .andExpect(status().isBadRequest());
        assertEquals(0, submissionCount());
    }

    @Test
    void sendingIsPersistentIdempotentAndOnlyUnlocksTheSelectedArea() throws Exception {
        fill(AREA, 1);
        String first = mvc.perform(post(areaPath() + "/submit").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.canSubmit").value(false)).andExpect(jsonPath("$.submittedAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String instant = JsonPath.read(first, "$.submittedAt");
        em.flush(); em.clear();
        mvc.perform(post(areaPath() + "/submit").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.submittedAt").value(instant));
        assertEquals(1, submissionCount());
        mvc.perform(get("/api/pmi/" + YEAR).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.areas[?(@.areaId == '" + AREA + "')].selfEvaluationSubmitted", contains(true)))
                .andExpect(jsonPath("$.areas[?(@.areaId == '" + OTHER_AREA + "')].selfEvaluationSubmitted", contains(false)));
        mvc.perform(put(pmiPath() + "/components/" + A).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"criticalFactors\":\"Factores\",\"urgency\":5,\"tendency\":5,\"impact\":5}"))
                .andExpect(status().isOk());
    }

    @Test
    void submittedAnswersAreFrozenWhileOtherAreasRemainEditable() throws Exception {
        fill(AREA, 1);
        submit();
        mvc.perform(put("/api/self-evaluations/" + YEAR + "/valuations/" + A).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"level\":4}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/self-evaluations/" + YEAR + "/valuations/" + component(1)).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"level\":2,\"evidenceNote\":\"Avance automático\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/self-evaluations/" + YEAR).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.valuations[?(@.componentId == '" + A + "')].level", contains(1)));
    }

    @Test
    void firstSubmissionRequiresAnOpenPeriodButRetryAfterClosingKeepsOriginalTimestamp() throws Exception {
        fill(AREA, 1);
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today.minusDays(2), today.minusDays(1)));
        mvc.perform(post(areaPath() + "/submit").with(user(principal(owner)))).andExpect(status().isForbidden());
        assertEquals(0, submissionCount());
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        String first = mvc.perform(post(areaPath() + "/submit").with(user(principal(owner))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String instant = JsonPath.read(first, "$.submittedAt");
        periods.delete(secretary.getId(), YEAR);
        mvc.perform(post(areaPath() + "/submit").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.submittedAt").value(instant));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        mvc.perform(put("/api/self-evaluations/" + YEAR + "/valuations/" + A).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"level\":4}"))
                .andExpect(status().isBadRequest());
        assertEquals(1, submissionCount());
    }

    @Test
    void anUnpublishedPeriodCannotReceiveAFreshSubmission() throws Exception {
        fill(AREA, 1);
        periods.delete(secretary.getId(), YEAR);
        mvc.perform(post(areaPath() + "/submit").with(user(principal(owner)))).andExpect(status().isForbidden());
        assertEquals(0, submissionCount());
    }

    @Test
    void ownershipRolesAndAuthenticationProtectStatusAndSubmission() throws Exception {
        fill(AREA, 1);
        mvc.perform(get(areaPath()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isOk());
        mvc.perform(get(areaPath()).with(user(principal(secretary)))).andExpect(status().isBadRequest());
        mvc.perform(get(areaPath()).with(user(principal(other))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post(areaPath() + "/submit").with(user(principal(secretary))))
                .andExpect(status().isForbidden());
        // El selector no permite enviar la evaluación ajena; other conserva su propio borrador incompleto.
        mvc.perform(post(areaPath() + "/submit").with(user(principal(other))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(areaPath())).andExpect(status().isUnauthorized());
        mvc.perform(post(areaPath() + "/submit")).andExpect(status().isUnauthorized());
        assertEquals(0, submissionCount());
    }

    private void fill(UUID areaId, int level) {
        catalog.findByState(UseState.ACTIVE).stream().filter(c -> c.getProcess().getArea().getId().equals(areaId))
                .forEach(c -> selfEvaluations.saveValuation(owner.getId(), YEAR, c.getId(), new ValuationRequest(level, null, null, null, null)));
    }
    private void submit() throws Exception { mvc.perform(post(areaPath() + "/submit").with(user(principal(owner)))).andExpect(status().isOk()); }
    private int submissionCount() {
        em.flush();
        UUID id = evaluations.findByEstablishment_IdAndYear(owner.getId(), YEAR).orElseThrow().getId();
        return jdbc.queryForObject("SELECT count(*) FROM self_evaluation_area_submission WHERE evaluation_id = ?", Integer.class, id);
    }
    private Establishment institution() {
        String suffix = UUID.randomUUID().toString();
        return institutions.save(new Establishment("Institución de prueba", "test-only", new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
    }
    private String areaPath() { return "/api/self-evaluations/" + YEAR + "/areas/" + AREA; }
    private String pmiPath() { return "/api/pmi/" + YEAR + "/areas/" + AREA; }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
    private static UUID component(int id) { return UUID.fromString("34002026-0003-4000-8000-%012d".formatted(id)); }
}
