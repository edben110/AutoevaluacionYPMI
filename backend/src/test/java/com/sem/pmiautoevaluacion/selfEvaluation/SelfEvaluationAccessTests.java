package com.sem.pmiautoevaluacion.selfEvaluation;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationTotalsResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import com.sem.pmiautoevaluacion.users.entity.EmailRecord;
import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SelfEvaluationAccessTests {
    @Autowired MockMvc mvc;
    @MockitoBean SelfEvaluationService service;

    @Test
    void secretaryReadsSelectedInstitutionRatherThanOwnUserId() throws Exception {
        UUID target = UUID.randomUUID();
        when(service.find(target, 2026)).thenReturn(draft());
        when(service.list(target)).thenReturn(List.of());

        mvc.perform(get("/api/self-evaluations/2026")
                .param("establishmentId", target.toString()).with(user(secretary())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.year").value(2026));
        mvc.perform(get("/api/self-evaluations")
                .param("establishmentId", target.toString()).with(user(secretary())))
                .andExpect(status().isOk()).andExpect(content().json("[]"));

        verify(service).find(target, 2026);
        verify(service).list(target);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/self-evaluations", "/api/self-evaluations/2026"})
    void secretaryMustSelectInstitutionForConsultation(String path) throws Exception {
        mvc.perform(get(path).with(user(secretary())))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void secretaryCannotCreateDraftOrChangeInstitutionValuation() throws Exception {
        mvc.perform(put("/api/self-evaluations/2026").with(user(secretary())))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/self-evaluations/2026/valuations/" + UUID.randomUUID())
                .with(user(secretary())).contentType(MediaType.APPLICATION_JSON).content("{\"level\":1}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void institutionCanReadOwnRecordsWithOptionalMatchingSelector() throws Exception {
        CustomUserDetails owner = institution();
        UUID ownerId = owner.getUser().getId();
        when(service.find(ownerId, 2026)).thenReturn(draft());
        when(service.list(ownerId)).thenReturn(List.of());

        mvc.perform(get("/api/self-evaluations/2026").with(user(owner)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/self-evaluations").with(user(owner)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/self-evaluations/2026").with(user(owner))
                .param("establishmentId", ownerId.toString()))
                .andExpect(status().isOk());
        verify(service, times(2)).find(ownerId, 2026);
        verify(service).list(ownerId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/self-evaluations", "/api/self-evaluations/2026"})
    void institutionCannotReadAnotherInstitutionByChangingSelector(String path) throws Exception {
        mvc.perform(get(path).with(user(institution()))
                .param("establishmentId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void institutionStillCreatesAndValuesItsOwnDraft() throws Exception {
        CustomUserDetails owner = institution();
        UUID ownerId = owner.getUser().getId();
        UUID componentId = UUID.randomUUID();
        when(service.createOrFind(ownerId, 2026)).thenReturn(draft());
        when(service.saveValuation(eq(ownerId), eq(2026), eq(componentId), any())).thenReturn(
                new ValuationResponse(componentId, (short) 2, "PERTINENCE", "Pertinencia",
                        true, false, true, true, null, null, "Fortaleza escrita", "Mejora escrita", Instant.now()));

        mvc.perform(put("/api/self-evaluations/2026").with(user(owner)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/self-evaluations/2026/valuations/" + componentId).with(user(owner))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"level":2,"strengths":"Fortaleza escrita","improvementOpportunities":"Mejora escrita"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.improvementChance").value(true))
                .andExpect(jsonPath("$.strength").value(false))
                .andExpect(jsonPath("$.strengthsAllowed").value(true))
                .andExpect(jsonPath("$.improvementOpportunitiesAllowed").value(true))
                .andExpect(jsonPath("$.strengths").value("Fortaleza escrita"))
                .andExpect(jsonPath("$.improvementOpportunities").value("Mejora escrita"));
        verify(service).createOrFind(ownerId, 2026);
        verify(service).saveValuation(eq(ownerId), eq(2026), eq(componentId), argThat(request ->
                "Fortaleza escrita".equals(request.strengths())
                        && "Mejora escrita".equals(request.improvementOpportunities())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"strengths", "improvementOpportunities"})
    void rejectsNarrativesLongerThanAllowedBeforeCallingService(String field) throws Exception {
        mvc.perform(put("/api/self-evaluations/2026/valuations/" + UUID.randomUUID())
                .with(user(institution())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":3,\"" + field + "\":\"" + "a".repeat(10001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void consultationSerializesEmptyPercentagesAsZero() throws Exception {
        CustomUserDetails owner = institution();
        when(service.find(owner.getUser().getId(), 2026)).thenReturn(draft());
        mvc.perform(get("/api/self-evaluations/2026").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.percentages.existence").value(0.0))
                .andExpect(jsonPath("$.totals.percentages.pertinence").value(0.0))
                .andExpect(jsonPath("$.totals.percentages.appropriation").value(0.0))
                .andExpect(jsonPath("$.totals.percentages.continuousImprovement").value(0.0));
    }

    @Test
    void anonymousAndOtherRolesCannotReadAutoevaluations() throws Exception {
        mvc.perform(get("/api/self-evaluations/2026"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/self-evaluations/2026").with(user("other").roles("OTHER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    private static SelfEvaluationResponse draft() {
        return new SelfEvaluationResponse(UUID.randomUUID(), 2026, "DRAFT", Instant.now(), Instant.now(),
                List.of(), new ValuationTotalsResponse(0, 0, 0, 0, 0));
    }

    private static CustomUserDetails secretary() {
        Secretary secretary = new Secretary("Secretaría", "test-only", new EmailRecord("secretary@local.test"));
        secretary.setId(UUID.randomUUID());
        return new CustomUserDetails(secretary);
    }

    private static CustomUserDetails institution() {
        Establishment institution = new Establishment("Institución", "test-only", new EmailRecord("ie@local.test"),
                "000000000001", "Rectoría");
        institution.setId(UUID.randomUUID());
        return new CustomUserDetails(institution);
    }
}
