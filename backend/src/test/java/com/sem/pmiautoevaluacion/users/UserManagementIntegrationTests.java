package com.sem.pmiautoevaluacion.users;

import com.jayway.jsonpath.JsonPath;
import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.*;
import com.sem.pmiautoevaluacion.selfEvaluation.service.*;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserManagementIntegrationTests {
    static final int YEAR = 8097;
    static final UUID COMPONENT = UUID.fromString("34002026-0003-4000-8000-000000000017");
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired EstablishmentRepository institutions;
    @Autowired PasswordEncoder encoder;
    @Autowired SelfEvaluationPeriodService periods;
    @Autowired SelfEvaluationService selfEvaluations;
    @Autowired SelfEvaluationRepository evaluations;
    @Autowired EntityManager em;
    Secretary secretary;
    Establishment owner, other;
    LocalDate today;

    @BeforeEach void fixtures() {
        secretary = users.save(new Secretary("Secretaría integradora", encoder.encode("fixture-before"), new EmailRecord(email())));
        owner = institution(); other = institution();
        today = LocalDate.now(ZoneId.of("America/Bogota"));
        periods.configure(secretary.getId(), YEAR, new SelfEvaluationPeriodRequest(today, today));
    }

    @Test void secretaryCreatesInstitutionWithEncodedPasswordAndReadyEmptyDraftForOpenPeriod() throws Exception {
        periods.configure(secretary.getId(), YEAR + 1, new SelfEvaluationPeriodRequest(today.plusDays(1), today.plusDays(2)));
        String dane = UUID.randomUUID().toString().substring(0,20), email = email();
        String body = """
                {"name":"Institución registrada","password":"fixture-created","email":"%s","daneCode":"%s","rector":"Rectoría"}
                """.formatted(email, dane);
        String response = mvc.perform(post("/api/users/establishments").with(user(principal(secretary)))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.daneCode").value(dane)).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(response,"$.id"));
        em.flush(); em.clear();
        var account = institutions.findById(id).orElseThrow();
        assertTrue(encoder.matches("fixture-created", account.getPassword()));
        var draft = evaluations.findByEstablishment_IdAndYear(id,YEAR).orElseThrow();
        assertTrue(draft.getValuations().isEmpty()); assertNotNull(draft.getPeriod());
        assertTrue(evaluations.findByEstablishment_IdAndYear(id,YEAR+1).isEmpty());
        mvc.perform(get("/api/self-evaluations/"+YEAR).with(user(principal(account))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valuations").isEmpty());
    }

    @Test void secretaryCreatesAnotherSecretaryWithoutExposingPassword() throws Exception {
        String email = email();
        var response = mvc.perform(post("/api/users/secretaries").with(user(principal(secretary)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nueva Secretaría\",\"email\":\""+email+"\",\"password\":\"fixture-created\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist()).andReturn().getResponse().getContentAsString();
        var account = users.findById(UUID.fromString(JsonPath.read(response,"$.id"))).orElseThrow();
        assertInstanceOf(Secretary.class,account); assertTrue(encoder.matches("fixture-created",account.getPassword()));
    }

    @Test void registrationRequiresSecretaryAndAuthentication() throws Exception {
        String body = "{\"name\":\"Nueva cuenta\",\"email\":\""+email()+"\",\"password\":\"fixture-created\",\"daneCode\":\"123-test-only\",\"rector\":\"Rectoría\"}";
        for(String path : new String[]{"/api/users/establishments","/api/users/secretaries"}) {
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
            mvc.perform(post(path).with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void registrationRejectsDuplicatesAndOversizedFields() throws Exception {
        String duplicateDane = "{\"name\":\"Cuenta\",\"email\":\""+email()+"\",\"password\":\"fixture\",\"daneCode\":\""+owner.getDaneCode()+"\",\"rector\":\"Rectoría\"}";
        mvc.perform(post("/api/users/establishments").with(user(principal(secretary))).contentType(MediaType.APPLICATION_JSON).content(duplicateDane))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/users/secretaries").with(user(principal(secretary))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cuenta\",\"email\":\""+owner.getEmail().getValue()+"\",\"password\":\"fixture\"}"))
                .andExpect(status().isBadRequest());
        String oversized = "{\"name\":\""+"x".repeat(151)+"\",\"email\":\""+email()+"\",\"password\":\"fixture\"}";
        mvc.perform(post("/api/users/secretaries").with(user(principal(secretary))).contentType(MediaType.APPLICATION_JSON).content(oversized))
                .andExpect(status().isBadRequest());
    }

    @Test void ownerUpdatesAccountAndCanLoginWithNewDaneAndPasswordWithoutLosingEvaluation() throws Exception {
        selfEvaluations.saveValuation(owner.getId(),YEAR,COMPONENT,new ValuationRequest(2,"Documento de prueba",null,"Fortaleza","Oportunidad"));
        UUID evaluationId = evaluations.findByEstablishment_IdAndYear(owner.getId(),YEAR).orElseThrow().getId();
        String email = email(), dane = UUID.randomUUID().toString().substring(0,20);
        String body = "{\"name\":\"Nombre actualizado\",\"email\":\""+email+"\",\"password\":\"fixture-after\",\"daneCode\":\""+dane+"\",\"rector\":\"Rector actualizado\"}";
        mvc.perform(patch("/api/me").with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nombre actualizado"))
                .andExpect(jsonPath("$.daneCode").value(dane)).andExpect(jsonPath("$.role").value("ESTABLISHMENT"))
                .andExpect(jsonPath("$.password").doesNotExist());
        em.flush(); em.clear();
        var updated = institutions.findById(owner.getId()).orElseThrow();
        assertTrue(encoder.matches("fixture-after",updated.getPassword()));
        assertEquals(email,updated.getEmail().getValue());
        var evaluation = evaluations.findByEstablishment_IdAndYear(owner.getId(),YEAR).orElseThrow();
        assertEquals(evaluationId,evaluation.getId()); assertEquals(1,evaluation.getValuations().size());
        mvc.perform(post("/api/auth/establishment/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"daneCode\":\""+dane+"\",\"password\":\"fixture-after\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test void partialUpdateOnlyChangesTheAuthenticatedAccountAndKeepsOmittedFields() throws Exception {
        String oldHash=owner.getPassword(),oldDane=owner.getDaneCode(),otherName=other.getName();
        mvc.perform(patch("/api/me").param("establishmentId",other.getId().toString()).with(user(principal(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Solo mi cuenta\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(owner.getId().toString()));
        em.flush(); em.clear();
        var updated=institutions.findById(owner.getId()).orElseThrow();
        assertEquals("Solo mi cuenta",updated.getName()); assertEquals(oldHash,updated.getPassword()); assertEquals(oldDane,updated.getDaneCode());
        assertEquals(otherName,institutions.findById(other.getId()).orElseThrow().getName());
    }

    @Test void updateRejectsDuplicateEmailAndDane() throws Exception {
        mvc.perform(patch("/api/me").with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+other.getEmail().getValue()+"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/me").with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"daneCode\":\""+other.getDaneCode()+"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void updateRejectsEmptyInvalidOrUnauthenticatedRequestsAndSecretarySpecificFields() throws Exception {
        for(String body : new String[]{"{}","{\"name\":\"  \"}","{\"email\":\"correo-invalido\"}","{\"rector\":\""+"x".repeat(151)+"\"}"}) {
            mvc.perform(patch("/api/me").with(user(principal(owner))).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/me").with(user(principal(secretary))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"daneCode\":\"codigo-institucion\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/me").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Sin sesión\"}"))
                .andExpect(status().isUnauthorized());
    }

    private Establishment institution() {
        String id=UUID.randomUUID().toString();
        return institutions.save(new Establishment("Institución para integración",encoder.encode("fixture-before"),new EmailRecord(email()),id.substring(0,20),"Rectoría"));
    }
    private String email() { return UUID.randomUUID()+"@local.test"; }
    private CustomUserDetails principal(User account) { return new CustomUserDetails(account); }
}
