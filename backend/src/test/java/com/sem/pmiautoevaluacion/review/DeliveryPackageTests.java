package com.sem.pmiautoevaluacion.review;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.review.service.*;
import com.sem.pmiautoevaluacion.review.dto.*;
import com.sem.pmiautoevaluacion.review.repository.DeliveryPackageReviewRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.*;
import com.sem.pmiautoevaluacion.selfEvaluation.service.*;
import com.sem.pmiautoevaluacion.pmi.service.PMIService;
import com.sem.pmiautoevaluacion.pmi.dto.PriorityComponentRequest;
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
import static java.math.BigDecimal.valueOf;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeliveryPackageTests {
    static final int YEAR = 8091;
    static final UUID AREA = UUID.fromString("34002026-0001-4000-8000-000000000002");
    static final UUID OTHER_AREA = UUID.fromString("34002026-0001-4000-8000-000000000001");
    static final UUID COMPONENT = UUID.fromString("34002026-0003-4000-8000-000000000017");
    @Autowired MockMvc mvc;
    @Autowired EstablishmentRepository institutions;
    @Autowired UserRepository users;
    @Autowired ComponentRepository catalog;
    @Autowired SelfEvaluationService evaluations;
    @Autowired SelfEvaluationAreaService submissions;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired DeliveryPackageService packages;
    @Autowired DeliveryPackageReviewRepository reviews;
    @Autowired ReviewCommentService comments;
    @Autowired PMIService pmi;
    @Autowired EntityManager em;
    Establishment owner, other;
    Secretary secretary;
    LocalDate today;

    @BeforeEach void fixtures() {
        owner = institution("Institución A"); other = institution("Institución B");
        secretary = users.save(new Secretary("Revisor de paquetes", "test-only", new EmailRecord(UUID.randomUUID() + "@local.test")));
        today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
    }

    @Test void groupsAreasAndPmiIntoOnePackagePerInstitutionAndYear() throws Exception {
        assertTrue(packages.list(owner.getId()).isEmpty());
        submit(owner, YEAR, AREA); submit(owner, YEAR, OTHER_AREA); submit(other, YEAR, AREA);
        pmi.saveComponent(owner.getId(), YEAR, AREA, COMPONENT, new PriorityComponentRequest("Apoyo pedagógico", valueOf(5), valueOf(5), valueOf(5)));
        var own = packages.list(owner.getId());
        assertEquals(1, own.size()); assertEquals(2, own.getFirst().areas().size());
        assertEquals(4, own.getFirst().expectedAreas()); assertFalse(own.getFirst().reviewed());
        assertEquals(1, own.getFirst().areas().stream().filter(d -> d.pmiId() != null).count());
        mvc.perform(get("/api/reviews/packages").with(user(principal(secretary))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.establishmentId == '" + owner.getId() + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.establishmentId == '" + other.getId() + "')]", hasSize(1)));
        mvc.perform(get("/api/reviews/packages").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].establishmentId").value(owner.getId().toString()));
    }

    @Test void reviewIsPersistentIdempotentAndIndependentFromInstitutionAnswers() throws Exception {
        submit(owner, YEAR, AREA);
        pmi.saveComponent(owner.getId(), YEAR, AREA, COMPONENT, new PriorityComponentRequest("Factor", valueOf(5), valueOf(5), valueOf(5)));
        em.flush(); em.clear();
        var before = evaluations.find(owner.getId(), YEAR);
        String version = current().version();
        mvc.perform(put(path()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":\"" + version + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewed").value(true))
                .andExpect(jsonPath("$.reviewedBy").value("Revisor de paquetes"));
        em.flush(); em.clear();
        var reviewed = current(); assertTrue(reviewed.reviewed()); assertNotNull(reviewed.reviewedAt());
        var repeated = packages.mark(secretary.getId(), owner.getId(), YEAR, version);
        assertEquals(reviewed.reviewedAt(), repeated.reviewedAt());
        assertEquals(before.valuations(), evaluations.find(owner.getId(), YEAR).valuations());
        assertEquals(1, reviews.findByEvaluation_Establishment_Id(owner.getId()).size());
        mvc.perform(get("/api/reviews/packages").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].reviewed").value(true));
    }

    @Test void newAreaInvalidatesReviewAndRejectsMarkingAnUnseenVersion() throws Exception {
        submit(owner, YEAR, AREA);
        var old = current(); packages.mark(secretary.getId(), owner.getId(), YEAR, old.version());
        submit(owner, YEAR, OTHER_AREA);
        assertFalse(current().reviewed()); assertTrue(current().changedSinceReview());
        mvc.perform(put(path()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":\"" + old.version() + "\"}"))
                .andExpect(status().isBadRequest());
        assertTrue(packages.mark(secretary.getId(), owner.getId(), YEAR, current().version()).reviewed());
    }

    @Test void startingAndEditingPmiInvalidateReviewedPackageButSemCommentsDoNot() {
        submit(owner, YEAR, AREA);
        packages.mark(secretary.getId(), owner.getId(), YEAR, current().version());
        comments.add(secretary.getId(), owner.getId(), YEAR, AREA, COMPONENT, new ReviewCommentRequest("Observación para la próxima entrega"));
        assertTrue(current().reviewed());
        pmi.saveComponent(owner.getId(), YEAR, AREA, COMPONENT, new PriorityComponentRequest("Factor inicial", valueOf(5), valueOf(5), valueOf(5)));
        assertFalse(current().reviewed()); assertTrue(current().changedSinceReview());
        em.flush(); em.clear();
        packages.mark(secretary.getId(), owner.getId(), YEAR, current().version());
        pmi.saveComponent(owner.getId(), YEAR, AREA, COMPONENT, new PriorityComponentRequest("Factor actualizado", valueOf(4), valueOf(5), valueOf(5)));
        assertFalse(current().reviewed());
    }

    @Test void canReturnToPendingAndThatChangeAlsoPersists() throws Exception {
        submit(owner, YEAR, AREA);
        packages.mark(secretary.getId(), owner.getId(), YEAR, current().version());
        mvc.perform(delete(path()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewed").value(false))
                .andExpect(jsonPath("$.reviewedAt").value(nullValue()));
        em.flush(); em.clear();
        assertFalse(current().reviewed()); assertFalse(current().changedSinceReview());
        assertTrue(reviews.findByEvaluation_Establishment_Id(owner.getId()).isEmpty());
    }

    @Test void newYearAndOtherInstitutionsDoNotInheritReviewAndSameYearResetClearsIt() {
        submit(owner, YEAR, AREA); submit(other, YEAR, AREA);
        packages.mark(secretary.getId(), owner.getId(), YEAR, current().version());
        assertFalse(packages.list(other.getId()).getFirst().reviewed());
        periods.configure(secretary.getId(), YEAR + 1, new SelfEvaluationPeriodRequest(today, today));
        submit(owner, YEAR + 1, AREA);
        assertFalse(packages.list(owner.getId()).stream().filter(p -> p.year() == YEAR + 1).findFirst().orElseThrow().reviewed());
        assertTrue(current().reviewed());
        periods.delete(secretary.getId(), YEAR);
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
        em.flush(); em.clear();
        submit(owner, YEAR, AREA);
        assertFalse(current().reviewed()); assertNull(current().reviewedAt());
    }

    @Test void reviewWritesRequireSecretaryExplicitInstitutionAndValidVersion() throws Exception {
        submit(owner, YEAR, AREA);
        String body = "{\"version\":\"" + current().version() + "\"}";
        mvc.perform(put(path()).with(user(principal(owner))).param("establishmentId", owner.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(delete(path()).with(user(principal(owner))).param("establishmentId", owner.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(put(path()).with(user(principal(secretary))).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(put(path()).with(user(principal(secretary))).param("establishmentId", owner.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":\"invalida\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(path()).with(user(principal(secretary))).param("establishmentId", other.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        mvc.perform(put(path()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reviews/packages")).andExpect(status().isUnauthorized());
        assertFalse(current().reviewed());
    }

    private DeliveryPackageResponse current() {
        return packages.list(owner.getId()).stream().filter(p -> p.year() == YEAR).findFirst().orElseThrow();
    }
    private void submit(Establishment institution, int year, UUID area) {
        catalog.findAll().stream().filter(c -> c.getProcess().getArea().getId().equals(area)).forEach(c ->
                evaluations.saveValuation(institution.getId(), year, c.getId(), new ValuationRequest(1, "Acta física", null, null, "Mejorar")));
        submissions.submit(institution.getId(), year, area);
    }
    private Establishment institution(String name) {
        var suffix = UUID.randomUUID().toString();
        return institutions.save(new Establishment(name, "test-only", new EmailRecord(suffix + "@local.test"), suffix.substring(0,20), "Rectoría"));
    }
    private String path() { return "/api/reviews/packages/" + YEAR + "/review"; }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
}
