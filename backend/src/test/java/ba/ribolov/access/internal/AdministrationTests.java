package ba.ribolov.access.internal;

import ba.ribolov.access.api.Role;
import ba.ribolov.access.internal.administration.*;
import ba.ribolov.access.internal.domain.*;
import ba.ribolov.organizations.api.OrganizationManagement;
import ba.ribolov.organizations.model.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdministrationTests {
    @Autowired EntityManager em;
    @Autowired AdministrationService service;
    @Autowired MockMvc mvc;
    private static final String ISSUER = "https://identity.example.test";

    @Test
    void systemAdminCreatesHierarchyAndAuditInSameTransaction() {
        var context = context(Role.SYSTEM_ADMIN, null, "system");
        var caller = caller("system");
        var ministry = service.createOrganization(caller, context.getId(),
                new OrganizationManagement.Create("Ministarstvo", OrganizationType.MINISTRY, null, null, null), "Osnivanje");
        var federation = service.createOrganization(caller, context.getId(),
                new OrganizationManagement.Create("Savez", OrganizationType.FEDERATION, null, null, ministry.id()), "Osnivanje");
        var association = service.createOrganization(caller, context.getId(),
                new OrganizationManagement.Create("Udruženje", OrganizationType.ASSOCIATION, null, null, federation.id()), "Osnivanje");
        assertThat(association.parent().id()).isEqualTo(federation.id());
        assertThat(service.events(caller, context.getId(), 0).items()).hasSize(3);
        assertThat(service.history(caller, context.getId(), association.id())).hasSize(1);
        assertThatThrownBy(() -> service.createOrganization(caller, context.getId(),
                new OrganizationManagement.Create("Pogrešno", OrganizationType.ASSOCIATION, null, null, ministry.id()), "Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changingParentPreservesPreviousAffiliationAndIncrementsVersion() {
        var context = context(Role.SYSTEM_ADMIN, null, "system");
        var first = organization("Prvi savez", OrganizationType.FEDERATION);
        var second = organization("Drugi savez", OrganizationType.FEDERATION);
        var child = organization("Udruženje", OrganizationType.ASSOCIATION);
        em.persist(new OrganizationAffiliation(child, first, Instant.now().minusSeconds(1000), null));
        em.flush();
        em.clear(); // Load existing rows as a real HTTP transaction would.
        var updated = service.changeParent(caller("system"), context.getId(), child.getId(),
                new OrganizationManagement.ChangeParent(child.getVersion(), second.getId(), null), "Promjena nadležnosti");
        assertThat(updated.parent().id()).isEqualTo(second.getId());
        assertThat(updated.version()).isEqualTo(1);
        assertThat(service.history(caller("system"), context.getId(), child.getId())).hasSize(2);
    }

    @Test
    void associationAdminCannotUseOtherOrganizationsOrEscalateRoles() {
        var mine = organization("A", OrganizationType.ASSOCIATION);
        var foreign = organization("B", OrganizationType.ASSOCIATION);
        var context = context(Role.ASSOCIATION_ADMIN, mine, "admin");
        var target = user("worker");
        em.flush();
        assertThat(service.organizations(caller("admin"), context.getId(), "", 0).items()).extracting(OrganizationManagement.View::id).containsExactly(mine.getId());
        assertThatThrownBy(() -> service.updateOrganization(caller("admin"), context.getId(), foreign.getId(),
                new OrganizationManagement.Update(foreign.getVersion(), "Neovlašteno", null, null), "Test")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.grant(caller("admin"), context.getId(),
                new AdminViews.Grant(target.getId(), mine.getId(), Role.ASSOCIATION_ADMIN, null, null, "Test")))
                .isInstanceOf(AccessDeniedException.class);
        var grant = service.grant(caller("admin"), context.getId(),
                new AdminViews.Grant(target.getId(), mine.getId(), Role.OPERATOR, null, null, "Dodjela operatera"));
        assertThat(grant.organization().id()).isEqualTo(mine.getId());
        var revoked = service.revoke(caller("admin"), context.getId(), grant.id(), new AdminViews.Revoke(grant.version(), "Prestanak ovlaštenja"));
        assertThat(revoked.revokedAt()).isNotNull();
    }

    @Test
    void missingMfaStaleAuthenticationAndForeignContextAreDenied() {
        var context = context(Role.SYSTEM_ADMIN, null, "system");
        assertThatThrownBy(() -> service.users(new AdminCaller(ISSUER, "system", "1", Instant.now()), context.getId(), "", 0))
                .isInstanceOf(ReauthenticationRequired.class);
        assertThatThrownBy(() -> service.createUser(new AdminCaller(ISSUER, "system", "2", Instant.now().minusSeconds(301)), context.getId(),
                new AdminViews.CreateUser("new", "Novi korisnik", "Test"))).isInstanceOf(ReauthenticationRequired.class);
        assertThatThrownBy(() -> service.users(caller("stranger"), context.getId(), "", 0)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void roleContextIsExplicitAndPrivilegesAreNotCombined() {
        var organization = organization("A", OrganizationType.ASSOCIATION);
        var context = context(Role.SYSTEM_ADMIN, null, "system");
        var restricted = new RoleAssignment(context.getUser(), Role.OPERATOR, organization, Instant.now().minusSeconds(60), null);
        em.persist(restricted); em.flush();
        assertThatThrownBy(() -> service.users(caller("system"), restricted.getId(), "", 0)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void httpWritesRequireCsrfAndFreshMfa() throws Exception {
        var invitationOrg = organization("Pozvano udruzenje", OrganizationType.ASSOCIATION);
        var context = context(Role.SYSTEM_ADMIN, null, "system");
        mvc.perform(post("/api/v1/admin/invitations").header("X-Access-Context", context.getId())
                        .with(oidcLogin().idToken(t -> t.subject("system").claim("iss", ISSUER).claim("acr", "2").claim("auth_time", Instant.now().getEpochSecond())))
                        .contentType("application/json").content("{\"email\":\"new@example.test\",\"role\":\"ASSOCIATION_ADMIN\",\"organizationId\":\""+invitationOrg.getId()+"\",\"reason\":\"Test\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/invitations").header("X-Access-Context", context.getId()).with(csrf())
                        .with(oidcLogin().idToken(t -> t.subject("system").claim("iss", ISSUER).claim("acr", "1")))
                        .contentType("application/json").content("{\"email\":\"new@example.test\",\"role\":\"ASSOCIATION_ADMIN\",\"organizationId\":\""+invitationOrg.getId()+"\",\"reason\":\"Test\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.reauthenticate").value(true));
        mvc.perform(post("/api/v1/admin/invitations").header("X-Access-Context", context.getId()).with(csrf())
                        .with(oidcLogin().idToken(t -> t.subject("system").claim("iss", ISSUER).claim("acr", "2").claim("auth_time", Instant.now().getEpochSecond())))
                        .contentType("application/json").content("{\"email\":\"new@example.test\",\"role\":\"ASSOCIATION_ADMIN\",\"organizationId\":\""+invitationOrg.getId()+"\",\"reason\":\"Test\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("new@example.test"));
    }

    @Test
    void staleUpdatesAreRejectedAndSystemAdministratorCannotBeDeactivated() {
        var context = context(Role.SYSTEM_ADMIN, null, "system");
        var org = organization("A", OrganizationType.ASSOCIATION);
        em.flush();
        assertThatThrownBy(() -> service.updateOrganization(caller("system"), context.getId(), org.getId(),
                new OrganizationManagement.Update(-1L, "Novo", null, null), "Test")).isInstanceOf(jakarta.persistence.OptimisticLockException.class);
        assertThatThrownBy(() -> service.updateUser(caller("system"), context.getId(), context.getUser().getId(),
                new AdminViews.UpdateUser(context.getUser().getVersion(), "Admin", false, "Test"))).isInstanceOf(IllegalArgumentException.class);
    }

    private AdminCaller caller(String subject) { return new AdminCaller(ISSUER, subject, "2", Instant.now()); }
    private ApplicationUser user(String subject) { var u = new ApplicationUser(ISSUER, subject); u.updateProfile(subject, true); em.persist(u); return u; }
    private Organization organization(String name, OrganizationType type) { var o = new Organization(name, type, null, null); em.persist(o); return o; }
    private RoleAssignment context(Role role, Organization org, String subject) {
        var grant = new RoleAssignment(user(subject), role, org, Instant.now().minusSeconds(60), null);
        em.persist(grant); em.flush(); return grant;
    }
}
