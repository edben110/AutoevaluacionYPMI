package com.sem.pmiautoevaluacion.pmi;

import com.jayway.jsonpath.JsonPath;
import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.service.*;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.users.entity.*;
import com.sem.pmiautoevaluacion.users.repository.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class PMIIndicatorTests {
    private static final int YEAR = 8081;
    private static final UUID AREA = UUID.fromString("34002026-0001-4000-8000-000000000002");
    private static final UUID OTHER_AREA = UUID.fromString("34002026-0001-4000-8000-000000000001");
    private static final UUID A = UUID.fromString("34002026-0003-4000-8000-000000000017");
    private static final UUID B = UUID.fromString("34002026-0003-4000-8000-000000000018");
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
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

    @BeforeEach
    void fixtures() throws Exception {
        owner = institution(); other = institution();
        secretary = users.save(new Secretary("Secretaría de prueba", "test-only", new EmailRecord(UUID.randomUUID() + "@local.test")));
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        for (Establishment account : List.of(owner, other)) {
            catalog.findByState(UseState.ACTIVE).stream().filter(c -> c.getProcess().getArea().getId().equals(AREA))
                    .forEach(c -> selfEvaluations.saveValuation(account.getId(), YEAR, c.getId(),
                            new ValuationRequest(c.getId().equals(A) || c.getId().equals(B) ? 1 : 4, null, null, null, null)));
            areaEvaluations.submit(account.getId(), YEAR, AREA);
            score(account, A, 5); score(account, B, 5);
        }
        // La ficha sigue disponible aunque termine la ventana de autoevaluación.
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today.minusDays(3), today.minusDays(1)));
        em.flush(); em.clear();
    }

    @Test
    void fiveNamedIndicatorsAcrossGoalsAndComponentsProduceFiveIndependentSheets() throws Exception {
        create(owner, A, List.of(goal("Meta A", row("Uno"), row("Repetido"), row("Repetido")), goal("Meta B", row("Cuatro"))));
        create(owner, B, List.of(goal("Meta C", row("Cinco"))));
        var result = mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicators", hasSize(5)))
                .andExpect(jsonPath("$.indicators[*].name", containsInAnyOrder("Uno", "Repetido", "Repetido", "Cuatro", "Cinco")))
                .andExpect(jsonPath("$.indicators[*].editable", everyItem(is(true))))
                .andExpect(jsonPath("$.unnamedRows").value(0)).andReturn();
        List<String> ids = JsonPath.read(result.getResponse().getContentAsString(), "$.indicators[*].id");
        assertEquals(5, new HashSet<>(ids).size());
        assertEquals(5, jdbc.queryForObject("SELECT count(*) FROM pmi_indicator WHERE id IN (SELECT indicator_id FROM pmi_activity WHERE goal_id IN (SELECT g.id FROM pmi_goal g JOIN pmi_objective o ON o.id=g.objective_id JOIN prioritary_component pc ON pc.id=o.prioritary_component_id JOIN pmi p ON p.id=pc.pmi_id JOIN self_evaluation e ON e.id=p.self_evaluation_id WHERE e.establishment_id=?))", Integer.class, owner.getId()));
    }

    @Test
    void savesEveryDefinitionFieldAndReloadsItWithoutChangingTheAnexo3Row() throws Exception {
        var sheet = firstSheet(create(owner, A, List.of(goal("Meta propia", row("Sesiones realizadas")))));
        String id = (String) sheet.get("id");
        mvc.perform(put(path() + "/" + id).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content(json(definition())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.name").value("Sesiones realizadas"));
        em.flush(); em.clear();
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicators[0].definition.type").value("PROCESS"))
                .andExpect(jsonPath("$.indicators[0].definition.description").value("Medir el avance"))
                .andExpect(jsonPath("$.indicators[0].definition.measureUnit").value("Porcentaje (%)"))
                .andExpect(jsonPath("$.indicators[0].definition.variablesDefinition").value("R: realizadas\nP: programadas"))
                .andExpect(jsonPath("$.indicators[0].definition.dataFormula").value("(R / P) * 100"))
                .andExpect(jsonPath("$.indicators[0].definition.dataSource").value("Actas físicas y digitales"))
                .andExpect(jsonPath("$.indicators[0].definition.methodologicalAspects").value("Verificar las actas"))
                .andExpect(jsonPath("$.indicators[0].definition.calculationFrequency").value("Trimestral"))
                .andExpect(jsonPath("$.indicators[0].definition.measurementResponsible").value("Coordinación"));
        mvc.perform(get(formulation()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.objectives[0].goals[0].rows[0].indicatorName").value("Sesiones realizadas"))
                .andExpect(jsonPath("$.objectives[0].goals[0].rows[0].collectionFrequency").value("Mensual"));
    }

    @Test
    void updatingTheAnexo3NameKeepsTheSameIndicatorAndItsDefinition() throws Exception {
        String created = create(owner, A, List.of(goal("Meta", row("Nombre inicial"))));
        String objectiveId = JsonPath.read(created, "$.objectives[0].id");
        String goalId = JsonPath.read(created, "$.objectives[0].goals[0].id");
        String rowId = JsonPath.read(created, "$.objectives[0].goals[0].rows[0].id");
        String indicatorId = (String) firstSheet(created).get("id");
        save(owner, indicatorId, definition());
        var renamed = row("Nombre corregido"); renamed.put("id", rowId);
        var goal = goal("Meta corregida", renamed); goal.put("id", goalId);
        mvc.perform(put(formulation() + "/objectives/" + objectiveId).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(A, List.of(goal)))))
                .andExpect(status().isOk());
        em.flush(); em.clear();
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicators", hasSize(1)))
                .andExpect(jsonPath("$.indicators[0].id").value(indicatorId))
                .andExpect(jsonPath("$.indicators[0].name").value("Nombre corregido"))
                .andExpect(jsonPath("$.indicators[0].definition.dataFormula").value("(R / P) * 100"));
    }

    @Test
    void blankRowsAreNotInventedAsIndicatorsAndUnnamedExistingDefinitionsRemainVisible() throws Exception {
        var rows = new ArrayList<Map<String, Object>>(); rows.add(row("Indicador")); rows.add(row("   "));
        String created = create(owner, A, List.of(Map.of("description", "Meta", "rows", rows)));
        String indicatorId = (String) firstSheet(created).get("id");
        save(owner, indicatorId, definition());
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(jsonPath("$.indicators", hasSize(1))).andExpect(jsonPath("$.unnamedRows").value(1));
        String objectiveId = JsonPath.read(created, "$.objectives[0].id");
        String goalId = JsonPath.read(created, "$.objectives[0].goals[0].id");
        String rowId = JsonPath.read(created, "$.objectives[0].goals[0].rows[0].id");
        var blank = row(null); blank.put("id", rowId);
        var goal = goal("Meta", blank); goal.put("id", goalId);
        mvc.perform(put(formulation() + "/objectives/" + objectiveId).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(A, List.of(goal)))))
                .andExpect(status().isOk());
        em.flush(); em.clear();
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicators", hasSize(1)))
                .andExpect(jsonPath("$.indicators[0].name").value(nullValue()))
                .andExpect(jsonPath("$.indicators[0].editable").value(false))
                .andExpect(jsonPath("$.indicators[0].definition.type").value("PROCESS"));
    }

    @Test
    void partialDraftNormalizesEmptyTextAndCanClearPreviousFields() throws Exception {
        String id = (String) firstSheet(create(owner, A, List.of(goal("Meta", row("Nombre"))))).get("id");
        save(owner, id, definition());
        mvc.perform(put(path() + "/" + id).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"  Pendiente de acuerdos  \",\"dataFormula\":\"  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.complete").value(false))
                .andExpect(jsonPath("$.definition.description").value("Pendiente de acuerdos"))
                .andExpect(jsonPath("$.definition.dataFormula").value(nullValue()))
                .andExpect(jsonPath("$.definition.type").value(nullValue()));
    }

    @Test
    void validatesTypeAndTextLimitsWithoutChangingSavedData() throws Exception {
        String id = (String) firstSheet(create(owner, A, List.of(goal("Meta", row("Nombre"))))).get("id");
        save(owner, id, definition());
        for (String field : List.of("description", "variablesDefinition", "dataFormula", "dataSource", "methodologicalAspects")) {
            mvc.perform(put(path() + "/" + id).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of(field, "x".repeat(10001))))).andExpect(status().isBadRequest());
        }
        for (String field : List.of("measureUnit", "calculationFrequency", "measurementResponsible")) {
            mvc.perform(put(path() + "/" + id).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of(field, "x".repeat(501))))).andExpect(status().isBadRequest());
        }
        mvc.perform(put(path() + "/" + id).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"AUTOMATIC\"}")).andExpect(status().isBadRequest());
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(jsonPath("$.indicators[0].definition.description").value("Medir el avance"));
    }

    @Test
    void institutionAreaYearAndRoleBoundariesProtectDefinitions() throws Exception {
        String id = (String) firstSheet(create(owner, A, List.of(goal("Meta", row("Privado"))))).get("id");
        mvc.perform(get(path())).andExpect(status().isUnauthorized());
        mvc.perform(get(path()).with(user(principal(other))).param("establishmentId", owner.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(get(path()).with(user(principal(secretary)))).andExpect(status().isBadRequest());
        mvc.perform(get(path()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicators[0].name").value("Privado"));
        mvc.perform(put(path() + "/" + id).with(user(principal(secretary))).contentType(MediaType.APPLICATION_JSON).content(json(definition())))
                .andExpect(status().isForbidden());
        mvc.perform(put(path() + "/" + id).with(user(principal(other))).contentType(MediaType.APPLICATION_JSON).content(json(definition())))
                .andExpect(status().isNotFound());
        mvc.perform(put(path().replace(AREA.toString(), OTHER_AREA.toString()) + "/" + id).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(definition()))).andExpect(status().isNotFound());
        evaluations.save(new SelfEvaluation(owner, YEAR - 1));
        mvc.perform(put(path().replace(String.valueOf(YEAR), String.valueOf(YEAR - 1)) + "/" + id).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(definition()))).andExpect(status().isNotFound());
    }

    @Test
    void historicalIndicatorsKeepDefinitionsButCannotBeEdited() throws Exception {
        String id = (String) firstSheet(create(owner, B, List.of(goal("Meta", row("Histórico"))))).get("id");
        save(owner, id, definition());
        score(owner, B, 1);
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicators[0].editable").value(false))
                .andExpect(jsonPath("$.indicators[0].definition.dataSource").value("Actas físicas y digitales"));
        mvc.perform(put(path() + "/" + id).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        score(owner, B, 5);
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(jsonPath("$.indicators[0].editable").value(true))
                .andExpect(jsonPath("$.indicators[0].definition.dataSource").value("Actas físicas y digitales"));
    }

    @Test
    void deletingTheSourceObjectiveRemovesItsIndicatorDefinitionWithoutOrphans() throws Exception {
        String created = create(owner, A, List.of(goal("Meta", row("Eliminar"))));
        String id = (String) firstSheet(created).get("id");
        save(owner, id, definition());
        String objectiveId = JsonPath.read(created, "$.objectives[0].id");
        mvc.perform(delete(formulation() + "/objectives/" + objectiveId).with(user(principal(owner)))).andExpect(status().isNoContent());
        em.flush(); em.clear();
        mvc.perform(get(path()).with(user(principal(owner)))).andExpect(jsonPath("$.indicators", hasSize(0)));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM pmi_indicator WHERE id=?", Integer.class, UUID.fromString(id)));
    }

    @Test
    void readingAnEmptyAreaDoesNotCreatePlansOrSheets() throws Exception {
        evaluations.save(new SelfEvaluation(owner, YEAR - 1));
        int count = jdbc.queryForObject("SELECT count(*) FROM pmi_indicator", Integer.class);
        mvc.perform(get(path().replace(String.valueOf(YEAR), String.valueOf(YEAR - 1))).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pmiId").value(nullValue()))
                .andExpect(jsonPath("$.canEdit").value(false)).andExpect(jsonPath("$.indicators", hasSize(0)));
        assertEquals(count, jdbc.queryForObject("SELECT count(*) FROM pmi_indicator", Integer.class));
    }

    private Map<String, Object> definition() {
        return Map.of("type", "PROCESS", "description", "Medir el avance", "measureUnit", "Porcentaje (%)",
                "variablesDefinition", "R: realizadas\nP: programadas", "dataFormula", "(R / P) * 100",
                "dataSource", "Actas físicas y digitales", "methodologicalAspects", "Verificar las actas",
                "calculationFrequency", "Trimestral", "measurementResponsible", "Coordinación");
    }
    private String create(Establishment account, UUID component, List<Map<String, Object>> goals) throws Exception {
        return mvc.perform(post(formulation() + "/objectives").with(user(principal(account)))
                .contentType(MediaType.APPLICATION_JSON).content(json(objective(component, goals))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
    private Map<String, Object> firstSheet(String ignoredCreationResponse) throws Exception {
        String body = mvc.perform(get(path()).with(user(principal(owner)))).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.indicators[0]");
    }
    private void save(Establishment account, String id, Map<String, Object> body) throws Exception {
        mvc.perform(put(path() + "/" + id).with(user(principal(account))).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk());
    }
    private void score(Establishment account, UUID component, int value) throws Exception {
        mvc.perform(put(base() + "/components/" + component).with(user(principal(account)))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("criticalFactors", "Factores críticos", "urgency", value, "tendency", value, "impact", value))))
                .andExpect(status().isOk());
    }
    private Map<String, Object> objective(UUID component, List<Map<String, Object>> goals) {
        return Map.of("componentId", component.toString(), "description", "Objetivo institucional", "goals", goals);
    }
    @SafeVarargs
    private final Map<String, Object> goal(String description, Map<String, Object>... rows) {
        var goal = new LinkedHashMap<String, Object>();
        goal.put("description", description); goal.put("rows", List.of(rows)); return goal;
    }
    private Map<String, Object> row(String name) {
        var row = new LinkedHashMap<String, Object>(); row.put("indicatorName", name); row.put("collectionFrequency", "Mensual"); return row;
    }
    private Establishment institution() {
        String suffix = UUID.randomUUID().toString();
        return institutions.save(new Establishment("Institución de prueba", "test-only", new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
    }
    private String json(Object body) { return mapper.writeValueAsString(body); }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
    private String base() { return "/api/pmi/" + YEAR + "/areas/" + AREA; }
    private String formulation() { return base() + "/formulation"; }
    private String path() { return base() + "/indicators"; }
}
