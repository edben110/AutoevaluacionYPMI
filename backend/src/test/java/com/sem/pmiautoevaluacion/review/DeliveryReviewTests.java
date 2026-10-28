package com.sem.pmiautoevaluacion.review;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.review.service.ReviewCommentService;
import com.sem.pmiautoevaluacion.review.dto.ReviewCommentRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.*;
import com.sem.pmiautoevaluacion.selfEvaluation.service.*;
import com.sem.pmiautoevaluacion.users.entity.*;
import com.sem.pmiautoevaluacion.users.repository.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
class DeliveryReviewTests {
    private static final int YEAR = 8085;
    private static final UUID AREA = UUID.fromString("34002026-0001-4000-8000-000000000002");
    private static final UUID OTHER_AREA = UUID.fromString("34002026-0001-4000-8000-000000000001");
    private static final UUID COMPONENT = component(17);
    @Autowired MockMvc mvc;
    @Autowired EstablishmentRepository institutions;
    @Autowired UserRepository users;
    @Autowired ComponentRepository catalog;
    @Autowired SelfEvaluationService evaluations;
    @Autowired SelfEvaluationAreaService areas;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired ReviewCommentService comments;
    @Autowired EntityManager em;
    Establishment owner, other;
    Secretary secretary;
    LocalDate today;

    @BeforeEach void fixtures() {
        owner = institution("Institución revisada"); other = institution("Otra institución");
        secretary = users.save(new Secretary("Revisora SEM", "test-only", new EmailRecord(UUID.randomUUID() + "@local.test")));
        today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
    }

    @Test void inboxContainsOnlySubmittedAreasAndOwnHistoryIsIsolated() throws Exception {
        mvc.perform(get("/api/reviews/deliveries").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        fillAndSubmit(owner);
        fillAndSubmit(other);
        mvc.perform(get("/api/reviews/deliveries").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].establishmentId").value(owner.getId().toString()))
                .andExpect(jsonPath("$[0].areaId").value(AREA.toString()))
                .andExpect(jsonPath("$[0].pmiId").value(nullValue()));
        mvc.perform(get("/api/reviews/deliveries").with(user(principal(secretary))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.establishmentId == '" + owner.getId() + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.establishmentId == '" + other.getId() + "')]", hasSize(1)));
    }

    @Test void secretaryCommentsPersistWithAuthorAndDoNotChangeTheSubmittedAnswers() throws Exception {
        fillAndSubmit(owner);
        em.flush(); em.clear();
        var before = evaluations.find(owner.getId(), YEAR);
        mvc.perform(post(commentPath()).with(user(principal(secretary)))
                .param("establishmentId", owner.getId().toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"  Explica la ubicación del acta física.  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.componentId").value(COMPONENT.toString()))
                .andExpect(jsonPath("$.body").value("Explica la ubicación del acta física."))
                .andExpect(jsonPath("$.authorId").value(secretary.getId().toString()))
                .andExpect(jsonPath("$.authorName").value("Revisora SEM"));
        em.flush(); em.clear();
        mvc.perform(get(base() + "/comments").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
        var after = evaluations.find(owner.getId(), YEAR);
        assertEquals(before.valuations(), after.valuations());
        assertEquals(before.updatedAt(), after.updatedAt());
        assertEquals("SUBMITTED", areas.find(owner.getId(), YEAR, AREA).status());
    }

    @Test void historyAndCommentsRemainAvailableDuringTheNextPeriodAndOutsideDates() throws Exception {
        fillAndSubmit(owner);
        comments.add(secretary.getId(), owner.getId(), YEAR, AREA, COMPONENT, new ReviewCommentRequest("Revisar soporte para la próxima entrega"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today.minusDays(2), today.minusDays(1)));
        periods.configure(secretary.getId(), YEAR + 1, new SelfEvaluationPeriodRequest(today, today));
        em.flush(); em.clear();
        mvc.perform(get(base()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.delivery.year").value(YEAR))
                .andExpect(jsonPath("$.components[?(@.componentId == '" + COMPONENT + "')].valuation.evidenceUrl", contains("Acta física en rectoría")));
        mvc.perform(get(base() + "/comments").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].body").value("Revisar soporte para la próxima entrega"));
        assertTrue(evaluations.find(owner.getId(), YEAR + 1).valuations().isEmpty());
        mvc.perform(get(base()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test void roleOwnershipAndAuthenticationProtectCommentsAndHistory() throws Exception {
        fillAndSubmit(owner);
        mvc.perform(get(base()).with(user(principal(other))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get(base() + "/comments").with(user(principal(other))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get(base()).with(user(principal(secretary)))).andExpect(status().isBadRequest());
        mvc.perform(post(commentPath()).with(user(principal(owner))).param("establishmentId", owner.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"No autorizado\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(commentPath()).with(user(principal(secretary)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Sin selector\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(base())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reviews/deliveries")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/self-evaluations/" + YEAR + "/valuations/" + COMPONENT).with(user(principal(secretary)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"level\":4}"))
                .andExpect(status().isForbidden());
    }

    @Test void cannotCommentDraftsOrComponentsFromAnotherAreaOrInstitution() throws Exception {
        fillAndSubmit(owner);
        mvc.perform(get(base()).with(user(principal(other)))).andExpect(status().isNotFound());
        mvc.perform(post(commentPath()).with(user(principal(secretary))).param("establishmentId", other.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Área sin enviar\"}"))
                .andExpect(status().isNotFound());
        evaluations.saveValuation(owner.getId(), YEAR, component(1), new ValuationRequest(1, null, null, null, null));
        mvc.perform(post(base() + "/components/" + component(1) + "/comments").with(user(principal(secretary)))
                .param("establishmentId", owner.getId().toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Otro componente\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/reviews/" + YEAR + "/areas/" + OTHER_AREA).with(user(principal(owner))))
                .andExpect(status().isNotFound());
    }

    @Test void blankOrOversizedCommentsAreRejectedWithoutSaving() throws Exception {
        fillAndSubmit(owner);
        for (String body : new String[]{"{\"body\":\"   \"}", "{\"body\":\"" + "x".repeat(10001) + "\"}", "{}"}) {
            mvc.perform(post(commentPath()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString())
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(get(base() + "/comments").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test void deactivatedCatalogComponentsDoNotDisappearFromSubmittedHistory() throws Exception {
        fillAndSubmit(owner);
        catalog.findById(COMPONENT).orElseThrow().deactivate();
        em.flush(); em.clear();
        mvc.perform(get(base()).with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.components[?(@.componentId == '" + COMPONENT + "')]", hasSize(1)));
    }

    @Test void deletingPeriodPreservesAeCommentsButRepublishingSameYearClearsThem() throws Exception {
        fillAndSubmit(owner);
        comments.add(secretary.getId(), owner.getId(), YEAR, AREA, COMPONENT, new ReviewCommentRequest("Observación de la entrega original"));
        periods.delete(secretary.getId(), YEAR);
        em.flush(); em.clear();
        mvc.perform(get(base() + "/comments").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        em.flush(); em.clear();
        assertTrue(evaluations.find(owner.getId(), YEAR).valuations().isEmpty());
        fillAndSubmit(owner);
        mvc.perform(get(base() + "/comments").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test void pmiAppearsInInboxAndSecretaryCanReadAllAnnexesButCannotWrite() throws Exception {
        fillAndSubmit(owner);
        String path = "/api/pmi/" + YEAR + "/areas/" + AREA;
        mvc.perform(put(path + "/components/" + COMPONENT).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"criticalFactors\":\"Apoyo pedagógico\",\"urgency\":5,\"tendency\":5,\"impact\":5}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reviews/deliveries").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].pmiId").isNotEmpty());
        for (String suffix : new String[]{"", "/formulation", "/indicators", "/follow-ups"}) {
            mvc.perform(get(path + suffix).with(user(principal(secretary))).param("establishmentId", owner.getId().toString()))
                    .andExpect(status().isOk());
        }
        mvc.perform(put(path + "/components/" + COMPONENT).with(user(principal(secretary)))
                .param("establishmentId", owner.getId().toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"criticalFactors\":\"Intento de edición\",\"urgency\":1,\"tendency\":1,\"impact\":1}"))
                .andExpect(status().isForbidden());
    }

    private void fillAndSubmit(Establishment institution) {
        catalog.findAll().stream().filter(c -> c.getProcess().getArea().getId().equals(AREA))
                .forEach(c -> evaluations.saveValuation(institution.getId(), YEAR, c.getId(),
                        new ValuationRequest(1, "Acta física en rectoría", null, null, "Mejorar acompañamiento")));
        areas.submit(institution.getId(), YEAR, AREA);
    }
    private Establishment institution(String name) {
        var suffix = UUID.randomUUID().toString();
        return institutions.save(new Establishment(name, "test-only", new EmailRecord(suffix + "@local.test"), suffix.substring(0,20), "Rectoría"));
    }
    private String base() { return "/api/reviews/" + YEAR + "/areas/" + AREA; }
    private String commentPath() { return base() + "/components/" + COMPONENT + "/comments"; }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
    private static UUID component(int id) { return UUID.fromString("34002026-0003-4000-8000-%012d".formatted(id)); }
}
