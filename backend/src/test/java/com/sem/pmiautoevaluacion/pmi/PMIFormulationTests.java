package com.sem.pmiautoevaluacion.pmi;

import com.jayway.jsonpath.JsonPath;
import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationPeriodService;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationAreaService;
import org.springframework.jdbc.core.JdbcTemplate;
import com.sem.pmiautoevaluacion.users.entity.*;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
class PMIFormulationTests {
    private static final int YEAR = 8076;
    private static final UUID AREA = UUID.fromString("34002026-0001-4000-8000-000000000002");
    private static final UUID A = UUID.fromString("34002026-0003-4000-8000-000000000017");
    private static final UUID B = UUID.fromString("34002026-0003-4000-8000-000000000018");
    private static final UUID C = UUID.fromString("34002026-0003-4000-8000-000000000019");
    @Autowired MockMvc mvc;
    @Autowired ComponentRepository catalog;
    @Autowired ObjectMapper mapper;
    @Autowired EstablishmentRepository institutions;
    @Autowired UserRepository users;
    @Autowired SelfEvaluationRepository evaluations;
    @Autowired SelfEvaluationService selfEvaluations;
    @Autowired SelfEvaluationAreaService areaEvaluations;
    @Autowired JdbcTemplate jdbc;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired EntityManager em;
    private Establishment owner;
    private Establishment other;
    private Secretary secretary;
    private LocalDate today;

    @BeforeEach
    void fixtures() throws Exception {
        owner = institution();
        other = institution();
        secretary = users.save(new Secretary("Secretaría de prueba", "test-only",
                new EmailRecord(UUID.randomUUID() + "@local.test")));
        today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        for (Establishment account : List.of(owner, other)) {
            catalog.findByState(UseState.ACTIVE).stream().filter(c -> c.getProcess().getArea().getId().equals(AREA))
                    .forEach(c -> value(account, c.getId(), 4));
        }
        value(owner, A, 1);
        value(owner, B, 1);
        value(owner, C, 3);
        value(other, A, 1);
        areaEvaluations.submit(owner.getId(), YEAR, AREA);
        areaEvaluations.submit(other.getId(), YEAR, AREA);
        for (Establishment account : List.of(owner, other)) {
            mvc.perform(put(basePath() + "/components/" + A).with(user(principal(account)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"criticalFactors\":\"Factores\",\"urgency\":5,\"tendency\":4,\"impact\":5}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(put(basePath() + "/components/" + B).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"criticalFactors\":\"Factores\",\"urgency\":5,\"tendency\":4,\"impact\":5}"))
                .andExpect(status().isOk());
        periods.configure(secretary.getId(), YEAR,
                new SelfEvaluationPeriodRequest(today.minusDays(3), today.minusDays(1)));
        em.flush();
        em.clear();
    }

    @Test
    void previewUsesSelectedComponentsWithoutInventingObjectivesOrWritingAnEmptyPlan() throws Exception {
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedComponents[*].componentId", containsInAnyOrder(A.toString(), B.toString())))
                .andExpect(jsonPath("$.objectives", hasSize(0)))
                .andExpect(jsonPath("$.totals.totalCost").value("0.00"));
        evaluations.save(new SelfEvaluation(owner, YEAR - 1));
        mvc.perform(get("/api/pmi/" + (YEAR - 1) + "/areas/" + AREA + "/formulation").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pmiId").value(nullValue()))
                .andExpect(jsonPath("$.selectedComponents", hasSize(0)));
    }

    @Test
    void savesSeveralIndicatorsPerGoalAndExactExcelCostsWithPendingAmounts() throws Exception {
        var first = row("Indicador uno", "Reunir al equipo\nSocializar acuerdos", "1500000.00", "1500000.00", "0.00");
        var second = row("Indicador dos", "Reunir al equipo\nSocializar acuerdos", "500000.00", "500000.00", null);
        var third = row("Indicador tres", null, "0.00", null, null);
        var response = create(owner, objective(A, "Fortalecer la gestión", goal("Meta uno", first, second, third),
                goal("Meta dos", row("Indicador cuatro", "Desarrollar acciones", "1000000.00", "1000000.00", null))));
        String id = read(response, "$.objectives[0].id");
        em.flush();
        em.clear();
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectives[0].id").value(id))
                .andExpect(jsonPath("$.objectives[0].goals", hasSize(2)))
                .andExpect(jsonPath("$.objectives[0].goals[0].rows", hasSize(3)))
                .andExpect(jsonPath("$.objectives[0].goals[0].rows[0].activities").value("Reunir al equipo\nSocializar acuerdos"))
                .andExpect(jsonPath("$.objectives[0].goals[0].rows[2].totalCost").value("0.00"))
                .andExpect(jsonPath("$.objectives[0].goals[0].rows[2].fseAmount").value(nullValue()))
                .andExpect(jsonPath("$.objectives[0].goals[0].rows[0].startDate").value("2026-02-03"))
                .andExpect(jsonPath("$.totals.totalCost").value("3000000.00"))
                .andExpect(jsonPath("$.totals.fseAmount").value("3000000.00"))
                .andExpect(jsonPath("$.totals.otherAmount").value("0.00"))
                .andExpect(jsonPath("$.totals.pendingAmountFields").value(4));
    }

    @Test
    void savesIncompleteDraftAndDoesNotImposeEqualityOfFundingAndCost() throws Exception {
        var pending = new LinkedHashMap<String, Object>();
        var response = create(owner, objective(A, null, goal(null, pending)));
        assertNotNull(read(response, "$.objectives[0].id"));
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectives[0].description").value(nullValue()))
                .andExpect(jsonPath("$.objectives[0].goals[0].rows[0].totalCost").value(nullValue()))
                .andExpect(jsonPath("$.totals.pendingAmountFields").value(3));
        create(owner, objective(B, "Otro objetivo", goal("Meta", row("Indicador", "Acciones", "200.00", "30.00", "40.00"))));
    }

    @Test
    void monetaryValuesRemainExactBeyondJavascriptSafeDecimalPrecision() throws Exception {
        var response = create(owner, objective(A, "Precisión", goal("Meta",
                row("Indicador", "Acción", "99999999999999.99", "0.01", "0.02"))));
        assertEquals("99999999999999.99", read(response, "$.objectives[0].goals[0].rows[0].totalCost"));
        assertEquals("99999999999999.99", read(response, "$.totals.totalCost"));
    }

    @Test
    void updatesMaintainChildIdsAndExplicitRemovalDoesNotTouchOtherObjectives() throws Exception {
        var initial = create(owner, objective(A, "Primero", goal("Meta", row("Indicador", "Acción", "1.10", null, null))));
        String objectiveId = read(initial, "$.objectives[0].id");
        String goalId = read(initial, "$.objectives[0].goals[0].id");
        String rowId = read(initial, "$.objectives[0].goals[0].rows[0].id");
        create(owner, objective(B, "Segundo", goal("Conservar meta", row("Otro indicador", "Otras acciones", "2.20", null, null))));
        var updatedRow = row("Indicador corregido", "Acción corregida", "1.30", null, null);
        updatedRow.put("id", rowId);
        var updatedGoal = goal("Meta corregida", updatedRow);
        updatedGoal.put("id", goalId);
        mvc.perform(put(path() + "/objectives/" + objectiveId).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(A, "Primero corregido", updatedGoal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectives[?(@.id == '" + objectiveId + "')].goals[0].id", contains(goalId)))
                .andExpect(jsonPath("$.objectives[?(@.id == '" + objectiveId + "')].goals[0].rows[0].id", contains(rowId)))
                .andExpect(jsonPath("$.objectives", hasSize(2)))
                .andExpect(jsonPath("$.totals.totalCost").value("3.50"));
        mvc.perform(put(path() + "/objectives/" + objectiveId).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(A, "Sin metas por ahora"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.totalCost").value("2.20"));
        em.flush();
        em.clear();
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.objectives", hasSize(2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1.00", "1.001", "100000000000000.00"})
    void rejectsNegativeOrOversizedOrFractionalCentAmounts(String amount) throws Exception {
        mvc.perform(post(path() + "/objectives").with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(objective(A, "No guardar", goal("Meta", row("Indicador", "Acción", amount, null, null))))))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.objectives", hasSize(0)));
    }

    @Test
    void rejectsInvertedDatesButAllowsOnlyOneDateWhileDrafting() throws Exception {
        var invalid = row("Indicador", "Acción", null, null, null);
        invalid.put("startDate", "2026-12-31");
        invalid.put("finishDate", "2026-01-01");
        mvc.perform(post(path() + "/objectives").with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(A, "No guardar", goal("Meta", invalid)))))
                .andExpect(status().isBadRequest());
        var partial = row("Indicador", "Acción", null, null, null);
        partial.put("finishDate", null);
        create(owner, objective(A, "Fecha parcial", goal("Meta", partial)));
    }

    @Test
    void deselectionPreservesHistoricalObjectiveAndExcludesItsAmounts() throws Exception {
        var response = create(owner, objective(B, "Conservar historial", goal("Meta", row("Indicador", "Acción", "500.00", null, null))));
        String id = read(response, "$.objectives[0].id");
        score(B, 1, 1, 1);
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectives[0].id").value(id))
                .andExpect(jsonPath("$.objectives[0].editable").value(false))
                .andExpect(jsonPath("$.totals.excludedObjectives").value(1))
                .andExpect(jsonPath("$.totals.totalCost").value("0.00"));
        mvc.perform(put(path() + "/objectives/" + id).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(B, "No modificar"))))
                .andExpect(status().isBadRequest());
        score(B, 5, 4, 5);
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.objectives[0].editable").value(true))
                .andExpect(jsonPath("$.totals.totalCost").value("500.00"));
    }

    @Test
    void selectionReviewBlocksNewFormulationAndUnselectedComponentsAreRejected() throws Exception {
        mvc.perform(post(path() + "/objectives").with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(C, "No candidato"))))
                .andExpect(status().isBadRequest());
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        em.flush();
        UUID evaluationId = evaluations.findByEstablishment_IdAndYear(owner.getId(), YEAR).orElseThrow().getId();
        jdbc.update("UPDATE component_valuation SET level = 3 WHERE evaluation_id = ? AND component_id IN (?, ?)", evaluationId, A, B);
        em.clear();
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canFormulate").value(false));
        mvc.perform(post(path() + "/objectives").with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(B, "Revisar selección primero"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validatesForeignOrDuplicateChildIdsWithoutStealingAnotherObjectivesGoals() throws Exception {
        var first = create(owner, objective(A, "Primero", goal("Meta", row("Indicador", "Acción", "1.00", null, null))));
        String firstId = read(first, "$.objectives[0].id");
        String goalId = read(first, "$.objectives[0].goals[0].id");
        String rowId = read(first, "$.objectives[0].goals[0].rows[0].id");
        var stolenGoal = goal("Meta ajena");
        stolenGoal.put("id", goalId);
        mvc.perform(post(path() + "/objectives").with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(B, "No reutilizar IDs", stolenGoal))))
                .andExpect(status().isBadRequest());
        var duplicate = row("Indicador", "Acción", "1.00", null, null);
        duplicate.put("id", rowId);
        var duplicateRows = goal("Meta", duplicate, duplicate);
        duplicateRows.put("id", goalId);
        mvc.perform(put(path() + "/objectives/" + firstId).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(A, "Duplicados", duplicateRows))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roleAndInstitutionChecksProtectCreateUpdateAndDelete() throws Exception {
        var response = create(owner, objective(A, "Objetivo privado"));
        String id = read(response, "$.objectives[0].id");
        mvc.perform(get(path()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get(path()).with(user(principal(secretary)))).andExpect(status().isBadRequest());
        mvc.perform(get(path()).with(user(principal(other))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(put(path() + "/objectives/" + id).with(user(principal(other)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(A, "No autorizado"))))
                .andExpect(status().isNotFound());
        mvc.perform(delete(path() + "/objectives/" + id).with(user(principal(other))))
                .andExpect(status().isNotFound());
        mvc.perform(post(path() + "/objectives").with(user(principal(secretary)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(A, "No autorizado"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path() + "/objectives/" + id).with(user(principal(secretary))))
                .andExpect(status().isForbidden());
        mvc.perform(get(path())).andExpect(status().isUnauthorized());
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.objectives[0].description").value("Objetivo privado"));
    }

    @Test
    void deleteIsExplicitAndWorksForAnObjectiveNoLongerSelected() throws Exception {
        var response = create(owner, objective(B, "Eliminar explícitamente", goal("Meta", row("Indicador", "Acción", "1.00", null, null))));
        String id = read(response, "$.objectives[0].id");
        score(B, 1, 1, 1);
        mvc.perform(delete(path() + "/objectives/" + id).with(user(principal(owner))))
                .andExpect(status().isNoContent());
        em.flush();
        em.clear();
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.objectives", hasSize(0)));
    }

    private String create(Establishment account, Map<String, Object> body) throws Exception {
        MvcResult result = mvc.perform(post(path() + "/objectives").with(user(principal(account)))
                .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().is2xxSuccessful()).andReturn();
        return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private void score(UUID id, int u, int t, int i) throws Exception {
        mvc.perform(put(basePath() + "/components/" + id).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"criticalFactors\":\"Factores\",\"urgency\":%d,\"tendency\":%d,\"impact\":%d}".formatted(u, t, i)))
                .andExpect(status().isOk());
    }

    @SafeVarargs
    private final Map<String, Object> objective(UUID componentId, String text, Map<String, Object>... goals) {
        var result = new LinkedHashMap<String, Object>();
        result.put("componentId", componentId.toString()); result.put("description", text); result.put("goals", List.of(goals));
        return result;
    }

    @SafeVarargs
    private final Map<String, Object> goal(String description, Map<String, Object>... rows) {
        var result = new LinkedHashMap<String, Object>();
        result.put("description", description); result.put("rows", List.of(rows));
        return result;
    }

    private Map<String, Object> row(String name, String activities, String cost, String fse, String otherFunding) {
        var result = new LinkedHashMap<String, Object>();
        result.put("indicatorName", name); result.put("activities", activities); result.put("totalCost", cost);
        result.put("fseAmount", fse); result.put("otherAmount", otherFunding); result.put("responsible", "Equipo institucional");
        result.put("startDate", "2026-02-03"); result.put("finishDate", "2026-12-17");
        result.put("collectionInstruments", "Actas y registros de participación"); result.put("collectionFrequency", "Mensual");
        return result;
    }

    private String json(Object body) { return mapper.writeValueAsString(body); }
    private String read(String json, String path) { return JsonPath.read(json, path); }
    private String basePath() { return "/api/pmi/" + YEAR + "/areas/" + AREA; }
    private String path() { return basePath() + "/formulation"; }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
    private Establishment institution() {
        String suffix = UUID.randomUUID().toString();
        return institutions.save(new Establishment("Institución de prueba", "test-only", new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
    }
    private void value(Establishment account, UUID componentId, int level) {
        selfEvaluations.saveValuation(account.getId(), YEAR, componentId,
                new ValuationRequest(level, null, "Evidencia", level == 1 ? null : "Fortaleza", level == 4 ? null : "Oportunidad"));
    }
}
