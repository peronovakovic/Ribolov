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