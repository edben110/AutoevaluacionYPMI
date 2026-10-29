package com.sem.pmiautoevaluacion.pmi;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.entity.*;
import com.sem.pmiautoevaluacion.pmi.repository.PMIRepository;
import com.sem.pmiautoevaluacion.pmi.service.*;
import com.sem.pmiautoevaluacion.review.service.DeliveryPackageService;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.*;
import com.sem.pmiautoevaluacion.selfEvaluation.service.*;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static java.math.BigDecimal.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PMISubmissionTests {
    private static final int YEAR = 8087;
    private static final UUID AREA = id(1, 2);
    private static final UUID OTHER_AREA = id(1, 1);
    private static final UUID A = id(3, 17);
    private static final UUID B = id(3, 18);
    @Autowired MockMvc mvc;
    @Autowired EstablishmentRepository institutions;
    @Autowired UserRepository users;
    @Autowired ComponentRepository catalog;
    @Autowired SelfEvaluationService evaluations;
    @Autowired SelfEvaluationAreaService areas;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired PMIService matrices;
    @Autowired PMIFormulationService formulation;
    @Autowired PMIIndicatorService indicators;
    @Autowired PMISubmissionService submissions;
    @Autowired PMIFollowUpService followUps;
    @Autowired DeliveryPackageService packages;
    @Autowired PMIRepository plans;
    @Autowired EntityManager em;
    Establishment owner, other;
    Secretary secretary;
    LocalDate today;

    @BeforeEach void fixtures() {
        owner = institution(); other = institution();
        secretary = users.save(new Secretary("Revisora", "test-only", new EmailRecord(UUID.randomUUID()+"@local.test")));
        today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today,today));
        fillArea(AREA, Set.of(A,B));
    }

    @Test void draftsArePrivateInEveryAnnexOverviewAndPackage() throws Exception {
        completePlan();
        for (String suffix : List.of("", "/formulation", "/indicators", "/follow-ups")) {
            mvc.perform(get(base()+suffix).with(user(principal(secretary))).param("establishmentId",owner.getId().toString()))
                    .andExpect(status().isNotFound());
            mvc.perform(get(base()+suffix).with(user(principal(owner)))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/pmi/"+YEAR).with(user(principal(secretary))).param("establishmentId",owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.areas",hasSize(0)));
        mvc.perform(get("/api/reviews/deliveries").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].pmiId").value(nullValue()));
        assertTrue(submissions.find(owner.getId(),YEAR,AREA).canSubmit());
    }

    @Test void finalSendIsExplicitPersistentIdempotentAndDoesNotNeedFollowUp() throws Exception {
        completePlan();
        assertEquals("PLANNING",followUps.find(owner.getId(),YEAR,AREA,null).phase());
        mvc.perform(post(base()+"/submission").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty()).andExpect(jsonPath("$.canSubmit").value(false));
        var first = submissions.find(owner.getId(),YEAR,AREA);
        em.flush(); em.clear();
        assertEquals(first,submissions.submit(owner.getId(),YEAR,AREA));
        for (String suffix : List.of("", "/formulation", "/indicators", "/follow-ups"))
            mvc.perform(get(base()+suffix).with(user(principal(secretary))).param("establishmentId",owner.getId().toString()))
                    .andExpect(status().isOk());
        mvc.perform(get(base()).with(user(principal(owner)))).andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(get(base()+"/formulation").with(user(principal(owner))))
                .andExpect(jsonPath("$.canFormulate").value(false)).andExpect(jsonPath("$.pmiSubmittedAt").isNotEmpty());
        assertFalse(indicators.find(owner.getId(),YEAR,AREA).canEdit());
        assertTrue(indicators.find(owner.getId(),YEAR,AREA).indicators().stream().noneMatch(IndicatorSheetResponse::editable));
        mvc.perform(get("/api/reviews/deliveries").with(user(principal(owner))))
                .andExpect(jsonPath("$[0].pmiId").isNotEmpty());
    }

    @Test void missingObjectivesRowsAndIndicatorSheetsPreventPublication() {
        var empty = submissions.find(owner.getId(),YEAR,AREA);
        assertFalse(empty.canSubmit());
        assertTrue(empty.pending().stream().anyMatch(p -> p.contains("Anexo 3")));
        assertThrows(BadRequestException.class,()->submissions.submit(owner.getId(),YEAR,AREA));
        for (UUID c : List.of(A,B)) formulation.createObjective(owner.getId(),YEAR,AREA,objective(c));
        var withoutSheets = submissions.find(owner.getId(),YEAR,AREA);
        assertFalse(withoutSheets.canSubmit());
        assertEquals(2,withoutSheets.pending().stream().filter(p -> p.startsWith("Anexo 4")).count());
        assertThrows(BadRequestException.class,()->submissions.submit(owner.getId(),YEAR,AREA));
        assertNull(plans.findBySelfEvaluation_Establishment_IdAndSelfEvaluation_YearAndArea_Id(owner.getId(),YEAR,AREA).orElseThrow().getSubmittedAt());
    }

    @Test void partialRowsCannotBeSentEvenWithACompleteIndicatorSheet() {
        var partial = new FormulationRowRequest(null,"Indicador",null,null,null,null,null,null,null,null,null);
        formulation.createObjective(owner.getId(),YEAR,AREA,new ObjectiveRequest(A,"Objetivo",List.of(new FormulationGoalRequest(null,"Meta",List.of(partial)))));
        for (var sheet : indicators.find(owner.getId(),YEAR,AREA).indicators()) indicators.save(owner.getId(),YEAR,AREA,sheet.id(),definition());
        var state = submissions.find(owner.getId(),YEAR,AREA);
        assertFalse(state.canSubmit());
        assertTrue(state.pending().stream().anyMatch(p -> p.contains("actividades") && p.contains("costo") && p.contains("fechas")));
        assertThrows(BadRequestException.class,()->submissions.submit(owner.getId(),YEAR,AREA));
    }

    @Test void sendingLocksEveryPlanningWriteIncludingHistoricalObjectiveDeletion() throws Exception {
        completePlan();
        var objective = formulation.find(owner.getId(),YEAR,AREA).objectives().getFirst();
        var sheet = indicators.find(owner.getId(),YEAR,AREA).indicators().getFirst();
        submissions.submit(owner.getId(),YEAR,AREA);
        assertThrows(BadRequestException.class,()->matrices.saveComponent(owner.getId(),YEAR,AREA,A,score()));
        assertThrows(BadRequestException.class,()->matrices.select(owner.getId(),YEAR,AREA,new PMISelectionRequest(List.of(A,B))));
        assertThrows(BadRequestException.class,()->formulation.createObjective(owner.getId(),YEAR,AREA,objective(A)));
        assertThrows(BadRequestException.class,()->formulation.updateObjective(owner.getId(),YEAR,AREA,objective.id(),objective(A)));
        assertThrows(BadRequestException.class,()->formulation.deleteObjective(owner.getId(),YEAR,AREA,objective.id()));
        assertThrows(BadRequestException.class,()->indicators.save(owner.getId(),YEAR,AREA,sheet.id(),definition()));
        mvc.perform(delete(base()+"/formulation/objectives/"+objective.id()).with(user(principal(owner))))
                .andExpect(status().isBadRequest());
    }

    @Test void newPeriodAllowsFollowUpOfTheSubmittedPlan() {
        completePlan(); submissions.submit(owner.getId(),YEAR,AREA);
        periods.configure(secretary.getId(),YEAR+1,new SelfEvaluationPeriodRequest(today,today));
        var control = followUps.find(owner.getId(),YEAR,AREA,today);
        assertEquals("FOLLOW_UP",control.phase()); assertTrue(control.canEdit());
        for (var row : control.rows()) followUps.save(owner.getId(),YEAR,AREA,today,row.rowId(),
                new FollowUpRequest(ExecutionStatus.EJ,new BigDecimal("40"),"Avance con acta física"));
        assertEquals("COMPLETED",followUps.complete(owner.getId(),YEAR,AREA,today).phase());
        assertEquals("SUBMITTED",submissions.find(owner.getId(),YEAR,AREA).status());
    }

    @Test void onlyPublicationChangesTheReceivedPackageAndOtherAreaRemainsPrivate() throws Exception {
        String version = packages.list(owner.getId()).getFirst().version();
        packages.mark(secretary.getId(),owner.getId(),YEAR,version);
        completePlan();
        assertTrue(packages.list(owner.getId()).getFirst().reviewed());
        fillArea(OTHER_AREA,Set.of(id(3,1)));
        // La nueva autoevaluación llega, pero ambos borradores de PMI siguen privados.
        var current = packages.list(owner.getId()).getFirst();
        packages.mark(secretary.getId(),owner.getId(),YEAR,current.version());
        submissions.submit(owner.getId(),YEAR,AREA);
        var received = packages.list(owner.getId()).getFirst();
        assertFalse(received.reviewed()); assertTrue(received.changedSinceReview());
        assertEquals(1,received.areas().stream().filter(a -> a.pmiId()!=null).count());
        mvc.perform(get("/api/pmi/"+YEAR).with(user(principal(secretary))).param("establishmentId",owner.getId().toString()))
                .andExpect(jsonPath("$.areas",hasSize(1))).andExpect(jsonPath("$.areas[0].areaId").value(AREA.toString()));
        mvc.perform(get(base().replace(AREA.toString(),OTHER_AREA.toString())).with(user(principal(secretary))).param("establishmentId",owner.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test void authenticationOwnershipAndSecretaryCannotPublishAnotherInstitution() throws Exception {
        completePlan();
        mvc.perform(post(base()+"/submission")).andExpect(status().isUnauthorized());
        mvc.perform(post(base()+"/submission").with(user(principal(secretary)))).andExpect(status().isForbidden());
        mvc.perform(post(base()+"/submission").with(user(principal(other))).param("establishmentId",owner.getId().toString()))
                .andExpect(status().isBadRequest());
        assertEquals("DRAFT",submissions.find(owner.getId(),YEAR,AREA).status());
    }

    private void fillArea(UUID area,Set<UUID> candidates) {
        catalog.findAll().stream().filter(c -> c.getProcess().getArea().getId().equals(area))
                .forEach(c -> evaluations.saveValuation(owner.getId(),YEAR,c.getId(),new ValuationRequest(candidates.contains(c.getId())?1:4,null,null,null,null)));
        areas.submit(owner.getId(),YEAR,area);
        for (UUID component : candidates) matrices.saveComponent(owner.getId(),YEAR,area,component,score());
    }
    private void completePlan() {
        for (UUID c : List.of(A,B)) formulation.createObjective(owner.getId(),YEAR,AREA,objective(c));
        for (var sheet : indicators.find(owner.getId(),YEAR,AREA).indicators()) indicators.save(owner.getId(),YEAR,AREA,sheet.id(),definition());
    }
    private ObjectiveRequest objective(UUID component) {
        var row = new FormulationRowRequest(null,"Talleres realizados","Desarrollar cuatro talleres",ZERO,ZERO,ZERO,"Coordinación",today,today.plusDays(1),"Actas y lista de asistencia","Mensual");
        return new ObjectiveRequest(component,"Unificar acuerdos",List.of(new FormulationGoalRequest(null,"Realizar cuatro talleres",List.of(row))));
    }
    private IndicatorDefinitionRequest definition() {
        return new IndicatorDefinitionRequest(IndicatorType.PROCESS,"Medir talleres","Porcentaje","A realizados, B programados","A/B*100","Actas","Contar sin duplicados","Mensual","Coordinación");
    }
    private PriorityComponentRequest score() { return new PriorityComponentRequest("Factor crítico",valueOf(5),valueOf(5),valueOf(5)); }
    private Establishment institution() {
        String suffix=UUID.randomUUID().toString();
        return institutions.save(new Establishment("Institución de prueba","test-only",new EmailRecord(suffix+"@local.test"),suffix.substring(0,20),"Rectoría"));
    }
    private static UUID id(int kind,int number) { return UUID.fromString("34002026-000%d-4000-8000-%012d".formatted(kind,number)); }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
    private String base() { return "/api/pmi/"+YEAR+"/areas/"+AREA; }
}
