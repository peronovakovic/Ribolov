package ba.ribolov.access.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTests {
    @Autowired MockMvc mvc;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void authorizationRequestRequiresMfaFreshLoginAndVerifiedEmailScope() throws Exception {
        var response=mvc.perform(get("/oauth2/authorization/keycloak")).andExpect(status().is3xxRedirection()).andReturn().getResponse();
        var query=java.net.URLDecoder.decode(java.net.URI.create(response.getRedirectedUrl()).getRawQuery(),java.nio.charset.StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(query).contains("acr_values=2","max_age=300","email","openid");
        org.assertj.core.api.Assertions.assertThat(query).contains("\"essential\":true","\"values\":[\"2\"]");
    }

    @Test
    void issuedCsrfTokenWorksWithSignedClaimsAndInvalidTokensAreRejected() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        var identity = oidcLogin().idToken(token -> token.subject("csrf-recipient")
                .claim("iss", "https://identity.example.test").claim("acr", "2")
                .claim("auth_time", java.time.Instant.now()).claim("email", "recipient@example.test")
                .claim("email_verified", true));
        var response = mvc.perform(get("/api/v1/csrf").session(session).with(identity))
                .andExpect(status().isOk()).andReturn().getResponse();
        var csrf = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response.getContentAsString());
        var header = csrf.get("headerName").asString();
        var value = csrf.get("token").asString();
        mvc.perform(post("/api/v1/invitations/preview").session(session).with(identity)
                .contentType("application/json").content("{\"token\":\"invalid\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/invitations/preview").session(session).with(identity).header(header, "invalid")
                .contentType("application/json").content("{\"token\":\"invalid\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/invitations/preview").session(session).with(identity).header(header, value)
                .contentType("application/json").content("{\"token\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitationsRequireAuthenticationAndCsrfAndDirectGrantRoutesAreClosed() throws Exception {
        mvc.perform(post("/api/v1/invitations/accept").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json").content("{\"token\":\"invalid\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/invitations/accept").with(oidcLogin()).contentType("application/json").content("{\"token\":\"invalid\"}"))
                .andExpect(status().isForbidden());
        for(String path:java.util.List.of("/api/v1/admin/users","/api/v1/admin/roles")){
            mvc.perform(post(path).with(oidcLogin()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType("application/json").content("{}")).andExpect(status().isMethodNotAllowed());
        }
    }


    @Test
    void accessContextsRequireAuthenticationAndDoNotTrustIdentityProviderRoleClaims() throws Exception {
        mvc.perform(get("/api/v1/access-contexts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mobile/v1/access-contexts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/access-contexts").with(oidcLogin().idToken(token ->
                        token.subject("unknown").claim("iss", "https://identity.example.test").claim("roles", java.util.List.of("SYSTEM_ADMIN")))))
                .andExpect(status().isOk()).andExpect(content().json("[]"))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/mobile/v1/access-contexts").with(jwt().jwt(token ->
                        token.issuer("https://identity.example.test").subject("unknown"))))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void contextEndpointsTakeIdentityFromAuthenticationAndRejectAnotherUsersContext() throws Exception {
        var organization = new ba.ribolov.organizations.model.Organization("Testno udruženje",
                ba.ribolov.organizations.model.OrganizationType.ASSOCIATION, null, null);
        var user = new ba.ribolov.access.internal.domain.ApplicationUser("https://identity.example.test", "context-owner");
        var assignment = new ba.ribolov.access.internal.domain.RoleAssignment(user,
                ba.ribolov.access.api.Role.OPERATOR, organization, java.time.Instant.now().minusSeconds(60), null);
        entityManager.persist(organization);
        entityManager.persist(user);
        entityManager.persist(assignment);
        entityManager.flush();
        var stranger = oidcLogin().idToken(token -> token.subject("stranger").claim("iss", "https://identity.example.test"));
        mvc.perform(get("/api/v1/access-contexts").param("subject", "context-owner").with(stranger))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/v1/access-contexts/" + assignment.getId()).with(stranger))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/access-contexts/" + assignment.getId()).with(oidcLogin().idToken(token ->
                        token.subject("context-owner").claim("iss", "https://identity.example.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization.id").value(organization.getId().toString()))
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void anonymousApiRequestsAreUnauthorizedRatherThanRedirected() throws Exception {
        mvc.perform(get("/api/v1/session")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mobile/v1/session")).andExpect(status().isUnauthorized());
    }

    @Test
    void webSessionReturnsIdentityWithoutBusinessPrivileges() throws Exception {
        mvc.perform(get("/api/v1/session").with(oidcLogin().idToken(token ->
                        token.subject("person-1").claim("name", "Test Ribolovac"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("person-1"))
                .andExpect(jsonPath("$.roles").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void authenticationAloneDoesNotOpenBusinessEndpoints() throws Exception {
        mvc.perform(get("/api/mobile/v1/session").with(jwt())).andExpect(status().isOk());
        mvc.perform(get("/api/mobile/v1/permits").with(jwt())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/members").with(oidcLogin())).andExpect(status().isForbidden());
    }

    @Test
    void webLogoutRequiresCsrfProtection() throws Exception {
        mvc.perform(post("/logout").with(oidcLogin())).andExpect(status().isForbidden());
    }

    @Test
    void livenessIsPublicWithoutExposingDetails() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}