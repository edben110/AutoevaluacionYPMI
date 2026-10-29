package com.sem.pmiautoevaluacion.pmi;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.entity.*;
import com.sem.pmiautoevaluacion.pmi.service.*;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.*;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationPeriodRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.service.*;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.users.entity.*;
import com.sem.pmiautoevaluacion.users.repository.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PMIPeriodDeletionTests {
    private static final int YEAR = 8804;
    @Autowired MockMvc mvc;
    @Autowired ComponentRepository catalog;
    @Autowired EstablishmentRepository institutions;
    @Autowired UserRepository users;
    @Autowired SelfEvaluationRepository evaluations;
    @Autowired SelfEvaluationPeriodRepository periodRepository;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired SelfEvaluationService selfEvaluations;
    @Autowired SelfEvaluationAreaService areaEvaluations;
    @Autowired PMIService pmi;
    @Autowired PMIFormulationService formulation;
    @Autowired PMIIndicatorService indicators;
    @Autowired PMIFollowUpService followUps;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    private Establishment owner;
    private Establishment other;
    private Secretary secretary;
    private LocalDate today;
    private List<UUID> areas;

    @BeforeEach
    void fixtures() {
        owner = institution(); other = institution();
        secretary = users.save(new Secretary("Secretaría de prueba", "test-only", new EmailRecord(UUID.randomUUID() + "@local.test")));
        today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        periods.configure(secretary.getId(), YEAR + 1, new SelfEvaluationPeriodRequest(today, today));
        areas = catalog.findByState(UseState.ACTIVE).stream().map(c -> c.getProcess().getArea().getId()).distinct().sorted().toList();
        assertEquals(4, areas.size());
    }

    @Test
    void deletingPeriodRemovesAllFourAreasAndAllInstitutionsIncludingEveryAnnexButKeepsOtherYears() throws Exception {
        var removed = new ArrayList<Aggregate>();
        for (Establishment account : List.of(owner, other)) for (UUID area : areas) removed.add(aggregate(account, YEAR, area));
        var preserved = aggregate(owner, YEAR + 1, areas.getFirst());
        em.flush(); em.clear();
        var answers = selfEvaluations.find(owner.getId(), YEAR);
        mvc.perform(delete("/api/self-evaluation-periods/" + YEAR).with(user(principal(secretary)))).andExpect(status().isNoContent());
        em.flush(); em.clear();
        for (Aggregate aggregate : removed) assertAggregateExists(aggregate, false);
        assertAggregateExists(preserved, true);
        assertEquals(answers, selfEvaluations.find(owner.getId(), YEAR));
        assertTrue(evaluations.findByYear(YEAR).stream().allMatch(e -> e.getPeriod() == null));
        assertNotNull(evaluations.findByEstablishment_IdAndYear(owner.getId(), YEAR + 1).orElseThrow().getPeriod());
        assertEquals("SUBMITTED", areaEvaluations.find(owner.getId(), YEAR, areas.getFirst()).status());

        var first = removed.getFirst();
        String base = "/api/pmi/" + YEAR + "/areas/" + first.areaId();
        mvc.perform(get("/api/pmi/" + YEAR).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.periodConfigured").value(false));
        mvc.perform(get(base).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.canFormulate").value(false)).andExpect(jsonPath("$.periodConfigured").value(false));
        mvc.perform(put(base + "/components/" + first.componentId()).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"criticalFactors\":\"No recrear\",\"urgency\":5,\"tendency\":5,\"impact\":5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(base + "/formulation/objectives").with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"componentId\":\"" + first.componentId() + "\",\"goals\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(base + "/indicators/" + first.indicatorId()).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(put(base + "/follow-ups/" + today + "/rows/" + first.rowId()).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());

        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        assertTrue(pmi.overview(owner.getId(), YEAR).periodConfigured());
        for (Establishment account : List.of(owner, other)) {
            assertTrue(selfEvaluations.find(account.getId(), YEAR).valuations().isEmpty());
            for (UUID area : areas) {
                var draft = areaEvaluations.find(account.getId(), YEAR, area);
                assertEquals("DRAFT", draft.status());
                assertEquals(0, draft.valuedComponents());
                assertNull(draft.submittedAt());
                assertFalse(pmi.find(account.getId(), YEAR, area).canFormulate());
            }
        }
        mvc.perform(put(base + "/components/" + first.componentId()).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"criticalFactors\":\"Aún incompleta\",\"urgency\":5,\"tendency\":5,\"impact\":5}"))
                .andExpect(status().isBadRequest());
        assertAggregateExists(preserved, true);
        catalog.findByState(UseState.ACTIVE).stream().filter(c -> c.getProcess().getArea().getId().equals(first.areaId()))
                .forEach(c -> selfEvaluations.saveValuation(owner.getId(), YEAR, c.getId(), new ValuationRequest(
                        c.getId().equals(first.componentId()) ? 1 : 4, null, null, null, null)));
        areaEvaluations.submit(owner.getId(), YEAR, first.areaId());
        var restarted = pmi.saveComponent(owner.getId(), YEAR, first.areaId(), first.componentId(), score());
        assertNotEquals(first.planId(), restarted.id());
        assertTrue(formulation.find(owner.getId(), YEAR, first.areaId()).objectives().isEmpty());
        assertTrue(followUps.find(owner.getId(), YEAR, first.areaId(), today).availableDates().isEmpty());
        for (Aggregate aggregate : removed) assertAggregateExists(aggregate, false);
    }

    @Test
    void publishingANewPeriodClearsLegacyAnswersSubmissionsAndPmiLeftWithoutAPeriod() {
        var legacy = aggregate(owner, YEAR, areas.getFirst());
        var otherYear = aggregate(owner, YEAR + 1, areas.getFirst());
        // Reproduce datos conservados por una versión anterior de la eliminación del período.
        evaluations.findByYear(YEAR).forEach(evaluation -> evaluation.removePeriod());
        evaluations.flush();
        periodRepository.deleteById(YEAR);
        periodRepository.flush();
        em.clear();
        assertAggregateExists(legacy, true);

        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        em.flush(); em.clear();
        assertAggregateExists(legacy, false);
        assertAggregateExists(otherYear, true);
        assertTrue(selfEvaluations.find(owner.getId(), YEAR).valuations().isEmpty());
        assertEquals("DRAFT", areaEvaluations.find(owner.getId(), YEAR, legacy.areaId()).status());
        assertNull(areaEvaluations.find(owner.getId(), YEAR, legacy.areaId()).submittedAt());
        assertFalse(pmi.find(owner.getId(), YEAR, legacy.areaId()).canFormulate());
    }

    @Test
    void editingDatesAndClosingTheWindowDoNotRemovePmi() {
        var saved = aggregate(owner, YEAR, areas.getFirst());
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today.minusDays(4), today.minusDays(1)));
        em.flush(); em.clear();
        assertAggregateExists(saved, true);
        assertTrue(pmi.find(owner.getId(), YEAR, saved.areaId()).canFormulate());
        assertTrue(pmi.find(owner.getId(), YEAR, saved.areaId()).periodConfigured());
        assertEquals("40.00", followUps.find(owner.getId(), YEAR, saved.areaId(), today).rows().getFirst().executionPercentage());
    }

    @Test
    void unauthorisedOrMissingPeriodDeletionDoesNotTouchExistingAnnexes() throws Exception {
        var saved = aggregate(owner, YEAR, areas.getFirst());
        mvc.perform(delete("/api/self-evaluation-periods/" + YEAR).with(user(principal(owner)))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/self-evaluation-periods/" + YEAR)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/self-evaluation-periods/" + (YEAR + 2)).with(user(principal(secretary)))).andExpect(status().isNotFound());
        em.flush(); em.clear();
        assertAggregateExists(saved, true);
        assertTrue(pmi.find(owner.getId(), YEAR, saved.areaId()).periodConfigured());
    }

    private Aggregate aggregate(Establishment account, int year, UUID area) {
        var components = catalog.findByState(UseState.ACTIVE).stream().filter(c -> c.getProcess().getArea().getId().equals(area))
                .sorted(Comparator.comparing(c -> c.getId())).toList();
        UUID componentId = components.getFirst().getId();
        components.forEach(component -> selfEvaluations.saveValuation(account.getId(), year, component.getId(),
                new ValuationRequest(component.getId().equals(componentId) ? 1 : 4, null, "Evidencia", null, null)));
        areaEvaluations.submit(account.getId(), year, area);
        var plan = pmi.saveComponent(account.getId(), year, area, componentId, score());
        var inputRow = new FormulationRowRequest(null, "Indicador", "Actividades", BigDecimal.TEN, null, null,
                "Responsable", today, today, "Actas", "Mensual");
        var result = formulation.createObjective(account.getId(), year, area, new ObjectiveRequest(componentId, "Objetivo",
                List.of(new FormulationGoalRequest(null, "Meta", List.of(inputRow)))));
        var objective = result.objectives().getFirst();
        var goal = objective.goals().getFirst();
        UUID rowId = goal.rows().getFirst().id();
        UUID indicatorId = indicators.find(account.getId(), year, area).indicators().getFirst().id();
        indicators.save(account.getId(), year, area, indicatorId, new IndicatorDefinitionRequest(IndicatorType.PROCESS,
                "Objetivo del indicador", "Porcentaje", "Variables", "R/P*100", "Actas", "Revisar actas", "Mensual", "Coordinación"));
        periods.configure(secretary.getId(), year + 1, new SelfEvaluationPeriodRequest(today, today));
        UUID followUpId = followUps.save(account.getId(), year, area, today, rowId, new FollowUpRequest(ExecutionStatus.EJ,
                new BigDecimal("40"), "Observaciones de la institución")).id();
        followUps.complete(account.getId(), year, area, today);
        UUID priorityId = jdbc.queryForObject("SELECT id FROM prioritary_component WHERE pmi_id=?", UUID.class, plan.id());
        return new Aggregate(area, componentId, plan.id(), priorityId, objective.id(), goal.id(), rowId, indicatorId, followUpId);
    }
    private void assertAggregateExists(Aggregate a, boolean exists) {
        Map<String, UUID> records = Map.of("pmi", a.planId(), "prioritary_component", a.priorityId(), "pmi_objective", a.objectiveId(),
                "pmi_goal", a.goalId(), "pmi_activity", a.rowId(), "pmi_indicator", a.indicatorId(), "pmi_follow_up", a.followUpId());
        records.forEach((table, id) -> assertEquals(exists ? 1 : 0,
                jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE id=?", Integer.class, id), table + " " + id));
    }
    private PriorityComponentRequest score() { return new PriorityComponentRequest("Factores críticos", BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(5)); }
    private Establishment institution() {
        String suffix = UUID.randomUUID().toString();
        return institutions.save(new Establishment("Institución de prueba", "test-only", new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
    }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
    private record Aggregate(UUID areaId, UUID componentId, UUID planId, UUID priorityId, UUID objectiveId, UUID goalId,
            UUID rowId, UUID indicatorId, UUID followUpId) {}
}
