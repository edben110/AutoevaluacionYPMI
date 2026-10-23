package com.sem.pmiautoevaluacion.pmi;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationPeriodService;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationAreaService;
import com.sem.pmiautoevaluacion.users.entity.EmailRecord;
import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
class PMIPrioritizationTests {
    private static final int YEAR = 8077;
    private static final UUID AREA = UUID.fromString("34002026-0001-4000-8000-000000000002");
    private static final UUID A = component(17);
    private static final UUID B = component(18);
    private static final UUID C = component(19);
    private static final UUID D = component(20);
    private static final UUID E = component(21);
    @Autowired MockMvc mvc;
    @Autowired ComponentRepository catalog;
    @Autowired EstablishmentRepository institutions;
    @Autowired UserRepository users;
    @Autowired SelfEvaluationRepository evaluations;
    @Autowired SelfEvaluationService selfEvaluations;
    @Autowired SelfEvaluationAreaService areaEvaluations;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    private Establishment owner;
    private Establishment other;
    private Secretary secretary;
    private LocalDate today;

    @BeforeEach
    void prepareSelfEvaluationsAndCloseTheirDiligenceWindow() {
        owner = institution();
        other = institution();
        secretary = users.save(new Secretary("Secretaría de prueba", "test-only",
                new EmailRecord(UUID.randomUUID() + "@local.test")));
        today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        fill(owner, 4);
        fill(other, 4);
        value(owner, A, 1);
        value(owner, B, 1);
        value(owner, C, 3);
        value(owner, D, 4);
        value(owner, E, 1);
        value(other, A, 1);
        areaEvaluations.submit(owner.getId(), YEAR, AREA);
        areaEvaluations.submit(other.getId(), YEAR, AREA);
        periods.configure(secretary.getId(), YEAR,
                new SelfEvaluationPeriodRequest(today.minusDays(3), today.minusDays(1)));
        em.flush();
        em.clear();
    }

    @Test
    void previewDerivesOnlyStateCandidatesAndDoesNotCreateDraft() throws Exception {
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(nullValue()))
                .andExpect(jsonPath("$.year").value(YEAR))
                .andExpect(jsonPath("$.projectionYears").value(1))
                .andExpect(jsonPath("$.filledOn").value(nullValue()))
                .andExpect(jsonPath("$.components", hasSize(3)))
                .andExpect(jsonPath("$.components[*].componentId", containsInAnyOrder(A.toString(), B.toString(), E.toString())))
                .andExpect(jsonPath("$.selectedComponentIds", hasSize(0)))
                .andExpect(jsonPath("$.selectionNeedsReview").value(false));
        assertEquals(0, planCount(owner));
    }

    @Test
    void persistsExcelTotalsAndSelectionWhileAutoevaluationPeriodIsClosed() throws Exception {
        score(A, 5, 4, 5); score(B, 3, 3, 4);
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.prioritizationComplete").value(false))
                .andExpect(jsonPath("$.selectedComponentIds", hasSize(0)));
        score(E, 4, 3, 5);
        em.flush(); em.clear();
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.filledOn").value(today.toString()))
                .andExpect(jsonPath("$.canFormulate").value(true))
                .andExpect(jsonPath("$.selectedComponentIds", contains(A.toString())))
                .andExpect(jsonPath("$.components[?(@.componentId == '" + A + "')].total", contains(14)))
                .andExpect(jsonPath("$.components[?(@.componentId == '" + B + "')].total", contains(10)))
                .andExpect(jsonPath("$.components[?(@.componentId == '" + E + "')].total", contains(12)));
        mvc.perform(put(path() + "/selection").with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(selection(A, E))).andExpect(status().isBadRequest());
        assertEquals(1, planCount(owner)); assertEquals(0, planCount(other));
    }

    @Test
    void automaticHighestAndExplicitChoiceResolveATieWithoutArbitraryReplacement() throws Exception {
        score(A, 4, 3, 5); score(B, 4, 3, 5); score(E, 5, 5, 5);
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedComponentIds", contains(E.toString())));
        score(E, 1, 1, 1);
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedComponentIds", containsInAnyOrder(A.toString(), B.toString())))
                .andExpect(jsonPath("$.selectionNeedsReview").value(false))
                .andExpect(jsonPath("$.canFormulate").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"criticalFactors\":\"Factores\",\"urgency\":0,\"tendency\":2,\"impact\":3}",
            "{\"criticalFactors\":\"Factores\",\"urgency\":2,\"tendency\":6,\"impact\":3}",
            "{\"criticalFactors\":\"Factores\",\"urgency\":2.5,\"tendency\":2,\"impact\":3}",
            "{\"criticalFactors\":\"Factores\",\"urgency\":2,\"tendency\":2}",
            "{\"criticalFactors\":\"   \",\"urgency\":2,\"tendency\":2,\"impact\":3}"})
    void rejectsInvalidFactorOrScoresBeforeCreatingPlan(String body) throws Exception {
        mvc.perform(put(path() + "/components/" + A).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertEquals(0, planCount(owner));
    }

    @Test
    void unresolvedTieDoesNotLeaveAdditionalMasqueradingAsPrimary() throws Exception {
        score(A, 5, 5, 5); score(B, 5, 5, 5); score(E, 5, 5, 5);
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.highestComponentIds", hasSize(3)))
                .andExpect(jsonPath("$.selectedComponentIds", hasSize(0)))
                .andExpect(jsonPath("$.selectionNeedsReview").value(true))
                .andExpect(jsonPath("$.canFormulate").value(false));
        for (String invalid : new String[] { selection(A), selection(A, A), selection(A, B, E), selection(A, C) }) {
            mvc.perform(put(path() + "/selection").with(user(principal(owner)))
                    .contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        mvc.perform(put(path() + "/selection").with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(selection(E, B))).andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedComponentIds", containsInAnyOrder(E.toString(), B.toString())))
                .andExpect(jsonPath("$.canFormulate").value(true));
        mvc.perform(put(path() + "/selection").with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(selection(A, E))).andExpect(status().isOk());
        em.flush(); jdbc.execute("SET CONSTRAINTS uq_priority_pmi_slot IMMEDIATE");
    }

    @ParameterizedTest
    @ValueSource(ints = {19, 20, 22})
    void cannotRateStrengthsOrAnUnvaluedComponent(int number) throws Exception {
        mvc.perform(put(path() + "/components/" + component(number)).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(scoreBody(2, 2, 2)))
                .andExpect(status().is(anyOf(is(400), is(404))));
        assertEquals(0, planCount(owner));
    }

    @Test
    void upsertingChangesOneRowAndPrimaryButKeepsSavedAdditional() throws Exception {
        score(A, 5, 4, 5); score(B, 2, 2, 2); score(E, 3, 3, 3);
        score(B, 5, 5, 5);
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedComponentIds", contains(B.toString())))
                .andExpect(jsonPath("$.components[?(@.criticalFactors != null)]", hasSize(3)));
        assertEquals(1, planCount(owner));
    }

    @Test
    void rejectsInvalidSelectionsWithoutChangingPreviouslySavedSelection() throws Exception {
        score(A, 5, 4, 5);
        score(B, 3, 3, 4);
        score(E, 4, 3, 5);
        for (String invalid : new String[] { selection(A, B, E), selection(A, A), selection(B), selection(A, C), "{\"componentIds\":[]}" }) {
            mvc.perform(put(path() + "/selection").with(user(principal(owner)))
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.selectedComponentIds", contains(A.toString())));
    }

    @Test
    void sourceStateChangeKeepsPriorFactorsButPreventsTheirSelection() throws Exception {
        score(A, 5, 4, 5); score(B, 3, 3, 4); score(E, 4, 3, 5);
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        changeSource(A, 3); em.flush(); em.clear();
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.components[?(@.componentId == '" + A + "')].eligible", contains(false)))
                .andExpect(jsonPath("$.components[?(@.componentId == '" + A + "')].total", contains(14)))
                .andExpect(jsonPath("$.selectedComponentIds", contains(E.toString())));
        mvc.perform(put(path() + "/components/" + A).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(scoreBody(1, 1, 1))).andExpect(status().isBadRequest());
    }

    @Test
    void secretaryReadsSelectedInstitutionButCannotAlterItsPrioritization() throws Exception {
        score(A, 5, 4, 5);
        mvc.perform(get(path()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.components[*].total", hasItem(14)));
        mvc.perform(get(path()).with(user(principal(secretary)))).andExpect(status().isBadRequest());
        mvc.perform(put(path() + "/components/" + A).with(user(principal(secretary)))
                .contentType(MediaType.APPLICATION_JSON).content(scoreBody(1, 1, 1))).andExpect(status().isForbidden());
        mvc.perform(put(path() + "/selection").with(user(principal(secretary)))
                .contentType(MediaType.APPLICATION_JSON).content(selection(A))).andExpect(status().isForbidden());
    }

    @Test
    void institutionsCannotReadOrWriteOtherOwnersAndAnonymousIsDenied() throws Exception {
        mvc.perform(get(path()).with(user(principal(owner))).param("establishmentId", other.getId().toString()))
                .andExpect(status().isForbidden());
        // La escritura siempre utiliza la institución de la sesión, aunque se envíe otro selector.
        mvc.perform(put(path() + "/components/" + A).with(user(principal(owner)))
                .param("establishmentId", other.getId().toString()).contentType(MediaType.APPLICATION_JSON)
                .content(scoreBody(2, 2, 2))).andExpect(status().isOk());
        assertEquals(1, planCount(owner));
        assertEquals(0, planCount(other));
        mvc.perform(get(path())).andExpect(status().isUnauthorized());
        mvc.perform(put(path() + "/selection").contentType(MediaType.APPLICATION_JSON).content(selection(A)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pmi/8078").with(user(principal(owner)))).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void fallsBackToTheLowestAvailableState(int level) throws Exception {
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        resetFixtureArea();
        fill(owner, 4); value(owner, A, level); value(owner, B, level);
        areaEvaluations.submit(owner.getId(), YEAR, AREA);
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateLevel").value(level))
                .andExpect(jsonPath("$.components[*].componentId", containsInAnyOrder(A.toString(), B.toString())));
        score(A, 4, 4, 5); score(B, 4, 4, 5);
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedComponentIds", containsInAnyOrder(A.toString(), B.toString())))
                .andExpect(jsonPath("$.canFormulate").value(true));
    }

    @Test
    void incompleteAreaBlocksDirectWritesAndAllFourMeansNoCandidates() throws Exception {
        UUID evaluationId = evaluations.findByEstablishment_IdAndYear(owner.getId(), YEAR).orElseThrow().getId();
        jdbc.update("DELETE FROM component_valuation WHERE evaluation_id = ? AND component_id = ?", evaluationId, C);
        em.clear();
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.selfEvaluationComplete").value(false))
                .andExpect(jsonPath("$.canFormulate").value(false));
        mvc.perform(put(path() + "/components/" + A).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(scoreBody(5, 5, 5))).andExpect(status().isBadRequest());
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        resetFixtureArea();
        fill(owner, 4);
        areaEvaluations.submit(owner.getId(), YEAR, AREA);
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.selfEvaluationComplete").value(true))
                .andExpect(jsonPath("$.components", hasSize(0)))
                .andExpect(jsonPath("$.candidateLevel").value(nullValue()))
                .andExpect(jsonPath("$.canFormulate").value(false));
    }

    @Test
    void areasHaveIndependentCandidatesAndPmisInTheSameYear() throws Exception {
        UUID directiva = UUID.fromString("34002026-0001-4000-8000-000000000001");
        String otherPath = "/api/pmi/" + YEAR + "/areas/" + directiva;
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        catalog.findByState(UseState.ACTIVE).stream().filter(c -> c.getProcess().getArea().getId().equals(directiva))
                .forEach(c -> value(owner, c.getId(), 4));
        value(owner, component(1), 2); value(owner, component(2), 2);
        areaEvaluations.submit(owner.getId(), YEAR, directiva);
        score(A, 5, 5, 5); score(B, 4, 4, 4); score(E, 4, 4, 4);
        for (UUID id : new UUID[] {component(1), component(2)}) {
            mvc.perform(put(otherPath + "/components/" + id).with(user(principal(owner)))
                    .contentType(MediaType.APPLICATION_JSON).content(scoreBody(4, 3, 4))).andExpect(status().isOk());
        }
        mvc.perform(get(otherPath).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateLevel").value(2))
                .andExpect(jsonPath("$.selectedComponentIds", containsInAnyOrder(component(1).toString(), component(2).toString())));
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedComponentIds", contains(A.toString())));
        mvc.perform(get("/api/pmi/" + YEAR).with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.areas", hasSize(4)));
        assertEquals(2, planCount(owner));
        mvc.perform(put(path() + "/components/" + component(1)).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(scoreBody(5, 5, 5)))
                .andExpect(status().is(anyOf(is(400), is(404))));
    }

    // Reconstruye el escenario de borrador dentro de la transacción de prueba, no es una reapertura pública.
    private void resetFixtureArea() {
        em.flush();
        UUID id = evaluations.findByEstablishment_IdAndYear(owner.getId(), YEAR).orElseThrow().getId();
        jdbc.update("DELETE FROM self_evaluation_area_submission WHERE evaluation_id = ? AND area_id = ?", id, AREA);
        em.clear();
    }

    private void changeSource(UUID componentId, int level) {
        em.flush();
        UUID id = evaluations.findByEstablishment_IdAndYear(owner.getId(), YEAR).orElseThrow().getId();
        jdbc.update("UPDATE component_valuation SET level = ? WHERE evaluation_id = ? AND component_id = ?", level, id, componentId);
        em.clear();
    }

    private void fill(Establishment institution, int level) {
        catalog.findByState(UseState.ACTIVE).stream().filter(c -> c.getProcess().getArea().getId().equals(AREA))
                .forEach(c -> value(institution, c.getId(), level));
    }

    private void score(UUID componentId, int urgency, int tendency, int impact) throws Exception {
        mvc.perform(put(path() + "/components/" + componentId).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(scoreBody(urgency, tendency, impact)))
                .andExpect(status().isOk());
    }

    private void value(Establishment institution, UUID componentId, int level) {
        selfEvaluations.saveValuation(institution.getId(), YEAR, componentId,
                new ValuationRequest(level, null, "Evidencia", level == 1 ? null : "Fortaleza",
                        level == 4 ? null : "Oportunidad"));
    }

    private int planCount(Establishment institution) {
        em.flush();
        UUID evaluationId = evaluations.findByEstablishment_IdAndYear(institution.getId(), YEAR).orElseThrow().getId();
        return jdbc.queryForObject("SELECT count(*) FROM pmi WHERE self_evaluation_id = ?", Integer.class, evaluationId);
    }

    private Establishment institution() {
        String suffix = UUID.randomUUID().toString();
        return institutions.save(new Establishment("Institución de prueba", "test-only",
                new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
    }

    private CustomUserDetails principal(com.sem.pmiautoevaluacion.users.entity.User account) { return new CustomUserDetails(account); }
    private String path() { return "/api/pmi/" + YEAR + "/areas/" + AREA; }
    private String scoreBody(int urgency, int tendency, int impact) {
        return "{\"criticalFactors\":\"Factores críticos\",\"urgency\":%d,\"tendency\":%d,\"impact\":%d}".formatted(urgency, tendency, impact);
    }
    private String selection(UUID... ids) {
        return "{\"componentIds\":[" + java.util.Arrays.stream(ids).map(id -> "\"" + id + "\"")
                .collect(java.util.stream.Collectors.joining(",")) + "]}";
    }
    private static UUID component(int suffix) { return UUID.fromString("34002026-0003-4000-8000-%012d".formatted(suffix)); }
}
