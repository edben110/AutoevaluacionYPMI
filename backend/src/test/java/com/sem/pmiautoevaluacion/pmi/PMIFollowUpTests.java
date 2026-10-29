package com.sem.pmiautoevaluacion.pmi;

import com.jayway.jsonpath.JsonPath;
import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.*;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.service.*;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.users.entity.*;
import com.sem.pmiautoevaluacion.users.repository.*;
import jakarta.persistence.EntityManager;
import java.time.*;
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
class PMIFollowUpTests {
    private static final int YEAR = 8083;
    private static final UUID AREA = UUID.fromString("34002026-0001-4000-8000-000000000002");
    private static final UUID OTHER_AREA = UUID.fromString("34002026-0001-4000-8000-000000000001");
    private static final UUID A = UUID.fromString("34002026-0003-4000-8000-000000000017");
    private static final UUID B = UUID.fromString("34002026-0003-4000-8000-000000000018");
    private static final String DATE = "2026-03-31";
    private static final String NEXT = "2026-06-30";
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
        catalog.findByState(UseState.ACTIVE).stream().filter(c -> c.getProcess().getArea().getId().equals(AREA))
                .forEach(c -> selfEvaluations.saveValuation(owner.getId(), YEAR, c.getId(),
                        new ValuationRequest(c.getId().equals(A) || c.getId().equals(B) ? 1 : 4, null, null, null, null)));
        areaEvaluations.submit(owner.getId(), YEAR, AREA);
        score(A, 5); score(B, 5);
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today.minusDays(3), today.minusDays(1)));
        em.flush(); em.clear();
    }

    @Test
    void planningAnnexIsReadOnlyUntilTheFollowingPeriodOpens() throws Exception {
        String rowId = firstRow(createDraft(List.of(row("Indicador", "Actividades"))));
        mvc.perform(get(path()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phase").value("PLANNING"))
                .andExpect(jsonPath("$.canEdit").value(false)).andExpect(jsonPath("$.rows[0].editable").value(false));
        mvc.perform(put(savePath(DATE, rowId)).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"executionStatus\":\"EJ\",\"executionPercentage\":40,\"observations\":\"Aún no\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path() + "/" + DATE + "/complete").with(user(principal(owner)))).andExpect(status().isBadRequest());
        openNextPeriod();
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner))))
                .andExpect(jsonPath("$.phase").value("FOLLOW_UP")).andExpect(jsonPath("$.canEdit").value(true));
    }

    @Test
    void nextEvaluationRequiresEveryFieldAndExplicitCompletionThenBecomesWritable() throws Exception {
        String created = create(List.of(row("Uno", "Actividades"), row("Dos", "Actividades")));
        List<String> ids = JsonPath.read(created, "$.objectives[0].goals[0].rows[*].id");
        String nextEvaluation = "/api/self-evaluations/" + (YEAR + 1);
        mvc.perform(get(nextEvaluation + "/prerequisites").with(user(principal(owner))))
                .andExpect(jsonPath("$.ready").value(false));
        mvc.perform(put(nextEvaluation).with(user(principal(owner)))).andExpect(status().isBadRequest());
        mvc.perform(put(nextEvaluation + "/valuations/" + A).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":2}")).andExpect(status().isBadRequest());
        mvc.perform(post(nextEvaluation + "/areas/" + AREA + "/submit").with(user(principal(owner)))).andExpect(status().isBadRequest());
        save(ids.get(0), DATE, Map.of("executionStatus", "EJ", "executionPercentage", "40", "observations", "Avance real, aún no finalizado"));
        save(ids.get(1), DATE, Map.of("executionStatus", "NI", "executionPercentage", "0"));
        mvc.perform(post(path() + "/" + DATE + "/complete").with(user(principal(owner)))).andExpect(status().isBadRequest());
        save(ids.get(1), DATE, Map.of("executionStatus", "NI", "executionPercentage", "0", "observations", "No inició por falta de recursos"));
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner))))
                .andExpect(jsonPath("$.canComplete").value(true));
        mvc.perform(get(nextEvaluation + "/prerequisites").with(user(principal(owner)))).andExpect(jsonPath("$.ready").value(false));
        String completed = mvc.perform(post(path() + "/" + DATE + "/complete").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phase").value("COMPLETED"))
                .andExpect(jsonPath("$.canEdit").value(false)).andReturn().getResponse().getContentAsString();
        String completedAt = JsonPath.read(completed, "$.completedAt");
        em.flush(); em.clear();
        mvc.perform(get(nextEvaluation + "/prerequisites").with(user(principal(owner)))).andExpect(jsonPath("$.ready").value(true));
        mvc.perform(put(nextEvaluation + "/valuations/" + A).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":2}")).andExpect(status().isOk());
        mvc.perform(put(savePath(DATE, ids.get(0))).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR + 1, new SelfEvaluationPeriodRequest(today.minusDays(3), today.minusDays(1)));
        mvc.perform(post(path() + "/" + NEXT + "/complete").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedAt").value(completedAt));
    }

    @Test
    void everyPreviousAreaMustBeCompletedAndOtherInstitutionsHaveIndependentAccess() throws Exception {
        String first = firstRow(create(List.of(row("Académico", "Actividad académica"))));
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        var areaComponents = catalog.findByState(UseState.ACTIVE).stream().filter(c -> c.getProcess().getArea().getId().equals(OTHER_AREA)).toList();
        UUID chosen = areaComponents.getFirst().getId();
        areaComponents.forEach(c -> selfEvaluations.saveValuation(owner.getId(), YEAR, c.getId(), new ValuationRequest(c.getId().equals(chosen) ? 1 : 4, null, null, null, null)));
        areaEvaluations.submit(owner.getId(), YEAR, OTHER_AREA);
        String otherBase = base().replace(AREA.toString(), OTHER_AREA.toString());
        mvc.perform(put(otherBase + "/components/" + chosen).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"criticalFactors\":\"Factores\",\"urgency\":5,\"tendency\":5,\"impact\":5}")).andExpect(status().isOk());
        String body = mvc.perform(post(otherBase + "/formulation/objectives").with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("componentId", chosen, "description", "Objetivo", "goals", List.of(Map.of("description", "Meta", "rows", List.of(row("Directivo", "Actividad directiva"))))))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String otherRow = firstRow(body);
        save(first, DATE, Map.of("executionStatus", "EJ", "executionPercentage", "35", "observations", "Avance académico"));
        mvc.perform(post(path() + "/" + DATE + "/complete").with(user(principal(owner)))).andExpect(status().isOk());
        String gate = "/api/self-evaluations/" + (YEAR + 1) + "/prerequisites";
        mvc.perform(get(gate).with(user(principal(owner)))).andExpect(jsonPath("$.ready").value(false)).andExpect(jsonPath("$.plans", hasSize(2)));
        mvc.perform(get(gate).with(user(principal(other)))).andExpect(jsonPath("$.ready").value(true));
        mvc.perform(put(otherBase + "/follow-ups/" + DATE + "/rows/" + otherRow).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"executionStatus\":\"CANC\",\"executionPercentage\":0,\"observations\":\"Actividad cancelada por cambio de contexto\"}"))
                .andExpect(status().isOk());
        mvc.perform(post(otherBase + "/follow-ups/" + DATE + "/complete").with(user(principal(owner)))).andExpect(status().isOk());
        mvc.perform(get(gate).with(user(principal(owner)))).andExpect(jsonPath("$.ready").value(true));
    }

    @Test
    void changingACompletedPlanRequiresANewCompleteControlBeforeContinuing() throws Exception {
        String created = create(List.of(row("Nombre inicial", "Actividades")));
        String rowId = firstRow(created);
        save(rowId, DATE, Map.of("executionStatus", "EJ", "executionPercentage", "40", "observations", "Avance inicial"));
        mvc.perform(post(path() + "/" + DATE + "/complete").with(user(principal(owner)))).andExpect(status().isOk());
        String objectiveId = JsonPath.read(created, "$.objectives[0].id"), goalId = JsonPath.read(created, "$.objectives[0].goals[0].id");
        var changed = row("Nombre actualizado", "Actividades actualizadas"); changed.put("id", rowId);
        mvc.perform(put(formulation() + "/objectives/" + objectiveId).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("componentId", A, "description", "Objetivo", "goals", List.of(Map.of("id", goalId, "description", "Meta", "rows", List.of(changed)))))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/self-evaluations/" + (YEAR + 1) + "/prerequisites").with(user(principal(owner))))
                .andExpect(jsonPath("$.ready").value(false));
        mvc.perform(post(path() + "/" + DATE + "/complete").with(user(principal(owner)))).andExpect(status().isBadRequest());
        save(rowId, NEXT, Map.of("executionStatus", "EJ", "executionPercentage", "50", "observations", "Control actualizado"));
        mvc.perform(post(path() + "/" + NEXT + "/complete").with(user(principal(owner)))).andExpect(status().isOk());
        mvc.perform(get("/api/self-evaluations/" + (YEAR + 1) + "/prerequisites").with(user(principal(owner))))
                .andExpect(jsonPath("$.ready").value(true));
    }

    @Test
    void finalizationAndPrerequisitesEnforceRolesAndOwnership() throws Exception {
        create(List.of(row("Indicador", "Actividades")));
        String gate = "/api/self-evaluations/" + (YEAR + 1) + "/prerequisites";
        mvc.perform(get(gate)).andExpect(status().isUnauthorized());
        mvc.perform(get(gate).with(user(principal(other))).param("establishmentId", owner.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(get(gate).with(user(principal(secretary)))).andExpect(status().isBadRequest());
        mvc.perform(get(gate).with(user(principal(secretary))).param("establishmentId", owner.getId().toString())).andExpect(status().isOk());
        mvc.perform(post(path() + "/" + DATE + "/complete").with(user(principal(secretary)))).andExpect(status().isForbidden());
        mvc.perform(post(path() + "/" + DATE + "/complete").with(user(principal(other)))).andExpect(status().isBadRequest());
        mvc.perform(post(path() + "/" + DATE + "/complete")).andExpect(status().isUnauthorized());
    }

    @Test
    void previewUsesEachAnexo3RowWithoutInventingDefaultStatesOrWritingControls() throws Exception {
        create(List.of(row("Indicador uno", "Reunir al equipo\nSocializar acuerdos"), row("Indicador repetido", null),
                row("Indicador repetido", "Acciones"), row(null, "Actividad sin indicador"), row(null, null)));
        int before = jdbc.queryForObject("SELECT count(*) FROM pmi_follow_up", Integer.class);
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rows", hasSize(4)))
                .andExpect(jsonPath("$.rows[*].id", everyItem(nullValue())))
                .andExpect(jsonPath("$.rows[*].executionStatus", everyItem(nullValue())))
                .andExpect(jsonPath("$.availableDates", hasSize(0)))
                .andExpect(jsonPath("$.rows[0].source.activities").value("Reunir al equipo\nSocializar acuerdos"))
                .andExpect(jsonPath("$.rows[0].source.startDate").value("2027-02-03"))
                .andExpect(jsonPath("$.rows[0].source.finishDate").value("2027-11-30"));
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM pmi_follow_up", Integer.class));
    }

    @Test
    void savesAllFiveManualStatesWithExactPercentagesAndInstitutionalObservations() throws Exception {
        String created = create(List.of(row("Uno", "Actividades"), row("Dos", "Actividades"), row("Tres", "Actividades"), row("Cuatro", "Actividades"), row("Cinco", "Actividades")));
        List<String> ids = JsonPath.read(created, "$.objectives[0].goals[0].rows[*].id");
        String[] statuses = {"NI", "ESP", "CANC", "FIN", "EJ"};
        String[] percentages = {"0", "10.25", "35.50", "100", "40.01"};
        for (int i = 0; i < ids.size(); i++) {
            save(ids.get(i), DATE, Map.of("executionStatus", statuses[i], "executionPercentage", percentages[i],
                    "observations", "Avances y dificultades\nActa física en rectoría y https://example.test/evidencia"));
        }
        em.flush(); em.clear();
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableDates", contains(DATE)))
                .andExpect(jsonPath("$.rows[*].executionStatus", contains("NI", "ESP", "CANC", "FIN", "EJ")))
                .andExpect(jsonPath("$.rows[*].executionPercentage", contains("0.00", "10.25", "35.50", "100.00", "40.01")))
                .andExpect(jsonPath("$.rows[4].observations").value("Avances y dificultades\nActa física en rectoría y https://example.test/evidencia"));
        mvc.perform(get(formulation()).with(user(principal(owner))))
                .andExpect(jsonPath("$.objectives[0].goals[0].rows[0].indicatorName").value("Uno"));
    }

    @Test
    void repeatedSaveUpdatesOneDateAndNewDatesKeepIndependentHistory() throws Exception {
        String rowId = firstRow(create(List.of(row("Indicador", "Actividades"))));
        String saved = save(rowId, DATE, Map.of("executionStatus", "EJ", "executionPercentage", "40", "observations", "Primer corte"));
        String id = JsonPath.read(saved, "$.id");
        String corrected = save(rowId, DATE, Map.of("executionStatus", "EJ", "executionPercentage", "45", "observations", "Corrección del primer corte"));
        assertEquals(id, JsonPath.read(corrected, "$.id"));
        save(rowId, NEXT, Map.of("executionStatus", "FIN", "executionPercentage", "100", "observations", "Segundo corte"));
        em.flush(); em.clear();
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner))))
                .andExpect(jsonPath("$.rows[0].executionPercentage").value("45.00"))
                .andExpect(jsonPath("$.rows[0].observations").value("Corrección del primer corte"))
                .andExpect(jsonPath("$.availableDates", contains(NEXT, DATE)));
        mvc.perform(get(path()).param("controlDate", NEXT).with(user(principal(owner))))
                .andExpect(jsonPath("$.rows[0].executionPercentage").value("100.00"));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM pmi_follow_up WHERE source_row_id=?", Integer.class, UUID.fromString(rowId)));
    }

    @Test
    void planEditsDoNotRewriteTheSnapshotOfAnEarlierControl() throws Exception {
        String created = create(List.of(row("Nombre inicial", "Actividades iniciales")));
        String rowId = firstRow(created);
        save(rowId, DATE, Map.of("observations", "Control original"));
        String objectiveId = JsonPath.read(created, "$.objectives[0].id");
        String goalId = JsonPath.read(created, "$.objectives[0].goals[0].id");
        var renamed = row("Nombre actualizado", "Actividades actualizadas"); renamed.put("id", rowId); renamed.put("startDate", "2027-03-10");
        var goal = new LinkedHashMap<String, Object>(); goal.put("id", goalId); goal.put("description", "Meta actualizada"); goal.put("rows", List.of(renamed));
        mvc.perform(put(formulation() + "/objectives/" + objectiveId).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("componentId", A.toString(), "description", "Objetivo actualizado", "goals", List.of(goal)))))
                .andExpect(status().isOk());
        em.flush(); em.clear();
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner))))
                .andExpect(jsonPath("$.rows[0].source.indicatorName").value("Nombre inicial"))
                .andExpect(jsonPath("$.rows[0].source.activities").value("Actividades iniciales"))
                .andExpect(jsonPath("$.rows[0].source.startDate").value("2027-02-03"))
                .andExpect(jsonPath("$.rows[0].sourceChanged").value(true));
        String next = save(rowId, NEXT, Map.of("executionPercentage", "80"));
        assertEquals("Nombre actualizado", JsonPath.read(next, "$.source.indicatorName"));
        String corrected = save(rowId, DATE, Map.of("observations", "Corregir solo avance"));
        assertEquals("Nombre inicial", JsonPath.read(corrected, "$.source.indicatorName"));
    }

    @Test
    void explicitSourceDeletionPreservesRecordedControlsForReadOnlyHistory() throws Exception {
        String created = create(List.of(row("Indicador", "Actividades")));
        String rowId = firstRow(created);
        save(rowId, DATE, Map.of("executionStatus", "EJ", "executionPercentage", "40", "observations", "Conservar evidencia histórica"));
        String objectiveId = JsonPath.read(created, "$.objectives[0].id");
        mvc.perform(delete(formulation() + "/objectives/" + objectiveId).with(user(principal(owner)))).andExpect(status().isNoContent());
        em.flush(); em.clear();
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].sourceRemoved").value(true))
                .andExpect(jsonPath("$.rows[0].editable").value(false))
                .andExpect(jsonPath("$.rows[0].source.indicatorName").value("Indicador"))
                .andExpect(jsonPath("$.rows[0].observations").value("Conservar evidencia histórica"));
        mvc.perform(put(savePath(DATE, rowId)).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void followUpCoversAllPlannedRowsEvenWhenTheirSelectionChanges() throws Exception {
        String rowId = firstRow(create(List.of(row("Indicador", "Actividades"))));
        save(rowId, DATE, Map.of("executionStatus", "EJ", "executionPercentage", "40"));
        score(A, 1);
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner))))
                .andExpect(jsonPath("$.rows[0].editable").value(true)).andExpect(jsonPath("$.rows[0].executionPercentage").value("40.00"));
        mvc.perform(put(savePath(NEXT, rowId)).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        score(A, 5);
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner)))).andExpect(jsonPath("$.rows[0].editable").value(true));
    }

    @Test
    void partialControlsDistinguishPendingFromZeroAndDoNotDeriveStatusFromPercentage() throws Exception {
        String rowId = firstRow(create(List.of(row("Indicador", "Actividades"))));
        String first = save(rowId, DATE, Map.of("executionPercentage", "0", "observations", "  Inicio  "));
        assertNull(JsonPath.read(first, "$.executionStatus"));
        assertEquals("0.00", JsonPath.read(first, "$.executionPercentage"));
        assertEquals("Inicio", JsonPath.read(first, "$.observations"));
        String cleared = save(rowId, DATE, Map.of("observations", "  "));
        assertNull(JsonPath.read(cleared, "$.executionPercentage"));
        assertNull(JsonPath.read(cleared, "$.observations"));
        assertNotNull(JsonPath.read(cleared, "$.id"));
    }

    @Test
    void rejectsInvalidPercentagesStatesDatesAndOversizedObservationsWithoutWriting() throws Exception {
        String rowId = firstRow(create(List.of(row("Indicador", "Actividades"))));
        for (String percentage : List.of("-0.01", "100.01", "1.001", "1000")) {
            mvc.perform(put(savePath(DATE, rowId)).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("executionPercentage", percentage)))).andExpect(status().isBadRequest());
        }
        mvc.perform(put(savePath(DATE, rowId)).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"executionStatus\":\"OTHER\"}")).andExpect(status().isBadRequest());
        mvc.perform(put(savePath(DATE, rowId)).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("observations", "x".repeat(10001))))).andExpect(status().isBadRequest());
        mvc.perform(get(path()).param("controlDate", "2026-02-30").with(user(principal(owner)))).andExpect(status().isBadRequest());
        mvc.perform(put(savePath("fecha-invalida", rowId)).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(owner))))
                .andExpect(jsonPath("$.rows[0].id").value(nullValue())).andExpect(jsonPath("$.availableDates", hasSize(0)));
    }

    @Test
    void ownerAreaYearAndRoleChecksProtectAllControls() throws Exception {
        String rowId = firstRow(create(List.of(row("Indicador privado", "Actividades"))));
        save(rowId, DATE, Map.of("observations", "Observación de la institución"));
        mvc.perform(get(path())).andExpect(status().isUnauthorized());
        mvc.perform(get(path()).with(user(principal(other))).param("establishmentId", owner.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(get(path()).with(user(principal(secretary)))).andExpect(status().isBadRequest());
        mvc.perform(get(path()).param("controlDate", DATE).with(user(principal(secretary))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(put(savePath(DATE, rowId)).with(user(principal(secretary))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(savePath(DATE, rowId)).with(user(principal(other))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(put(savePath(DATE, rowId).replace(AREA.toString(), OTHER_AREA.toString())).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isNotFound());
        evaluations.save(new SelfEvaluation(owner, YEAR - 1));
        mvc.perform(put(savePath(DATE, rowId).replace(String.valueOf(YEAR), String.valueOf(YEAR - 1))).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isNotFound());
    }

    @Test
    void emptyAreaReadDoesNotCreatePmiOrControlsAndDefaultsToBogotaDate() throws Exception {
        evaluations.save(new SelfEvaluation(owner, YEAR - 1));
        mvc.perform(get(path().replace(String.valueOf(YEAR), String.valueOf(YEAR - 1))).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pmiId").value(nullValue()))
                .andExpect(jsonPath("$.rows", hasSize(0))).andExpect(jsonPath("$.availableDates", hasSize(0)))
                .andExpect(jsonPath("$.controlDate").value(LocalDate.now(ZoneId.of("America/Bogota")).toString()));
    }

    private String create(List<Map<String, Object>> rows) throws Exception {
        String result = createDraft(rows);
        openNextPeriod();
        return result;
    }
    private String createDraft(List<Map<String, Object>> rows) throws Exception {
        return mvc.perform(post(formulation() + "/objectives").with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("componentId", A.toString(), "description", "Objetivo institucional", "goals", List.of(Map.of("description", "Meta institucional", "rows", rows))))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
    private void openNextPeriod() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR + 1, new SelfEvaluationPeriodRequest(today.withDayOfYear(1), today.withMonth(12).withDayOfMonth(31)));
    }
    private String save(String rowId, String date, Map<String, Object> body) throws Exception {
        return mvc.perform(put(savePath(date, rowId)).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
    private Map<String, Object> row(String name, String activities) {
        var row = new LinkedHashMap<String, Object>(); row.put("indicatorName", name); row.put("activities", activities);
        row.put("startDate", "2027-02-03"); row.put("finishDate", "2027-11-30"); return row;
    }
    private String firstRow(String created) { return JsonPath.read(created, "$.objectives[0].goals[0].rows[0].id"); }
    private void score(UUID component, int value) throws Exception {
        mvc.perform(put(base() + "/components/" + component).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("criticalFactors", "Factores críticos", "urgency", value, "tendency", value, "impact", value))))
                .andExpect(status().isOk());
    }
    private Establishment institution() {
        String suffix = UUID.randomUUID().toString();
        return institutions.save(new Establishment("Institución de prueba", "test-only", new EmailRecord(suffix + "@local.test"), suffix.substring(0, 20), "Rectoría"));
    }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
    private String json(Object body) { return mapper.writeValueAsString(body); }
    private String base() { return "/api/pmi/" + YEAR + "/areas/" + AREA; }
    private String formulation() { return base() + "/formulation"; }
    private String path() { return base() + "/follow-ups"; }
    private String savePath(String date, String rowId) { return path() + "/" + date + "/rows/" + rowId; }
}
