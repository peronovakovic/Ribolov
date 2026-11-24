package ba.ribolov.access.internal;

import ba.ribolov.access.api.Role;
import ba.ribolov.access.internal.application.AccessContextService;
import ba.ribolov.access.internal.domain.ApplicationUser;
import ba.ribolov.access.internal.domain.RoleAssignment;
import ba.ribolov.organizations.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaRelationshipsTests {
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    @Autowired EntityManager entityManager;

    @Test
    void persistsAndReloadsRealEntityRelationsInBothDirections() {
        var ministry = organization("Ministarstvo", OrganizationType.MINISTRY);
        var federation = organization("Savez", OrganizationType.FEDERATION);
        var association = organization("Udruženje", OrganizationType.ASSOCIATION);
        var top = new OrganizationAffiliation(federation, ministry, NOW, null);
        var bottom = new OrganizationAffiliation(association, federation, NOW, null);
        entityManager.persist(top);
        entityManager.persist(bottom);
        var user = user("issuer-a", "member-1");
        var assignment = new RoleAssignment(user, Role.OPERATOR, association, NOW, null);
        entityManager.persist(assignment);
        entityManager.flush();
        entityManager.clear();

        var loaded = entityManager.find(RoleAssignment.class, assignment.getId());
        assertThat(loaded.getUser().getSubject()).isEqualTo("member-1");
        assertThat(loaded.getOrganization().getName()).isEqualTo("Udruženje");
        assertThat(loaded.getUser().getRoleAssignments()).extracting(RoleAssignment::getId).containsExactly(assignment.getId());
        assertThat(loaded.getOrganization().getParentAffiliations()).singleElement().satisfies(link ->
                assertThat(link.getParent().getName()).isEqualTo("Savez"));
        assertThat(entityManager.find(Organization.class, ministry.getId()).getChildAffiliations())
                .singleElement().satisfies(link -> assertThat(link.getChild().getId()).isEqualTo(federation.getId()));
    }

    @Test
    void keepsPreviousParentInsteadOfOverwritingHistory() {
        var first = organization("Prvi savez", OrganizationType.FEDERATION);
        var second = organization("Drugi savez", OrganizationType.FEDERATION);
        var association = organization("Udruženje", OrganizationType.ASSOCIATION);
        var earlier = new OrganizationAffiliation(association, first, NOW.minusSeconds(3600), NOW);
        var current = new OrganizationAffiliation(association, second, NOW, null);
        entityManager.persist(earlier);
        entityManager.persist(current);
        entityManager.flush();
        entityManager.clear();
        var links = entityManager.find(Organization.class, association.getId()).getParentAffiliations();
        assertThat(links).hasSize(2);
        assertThat(links.stream().filter(link -> link.includes(NOW))).singleElement()
                .satisfies(link -> assertThat(link.getParent().getId()).isEqualTo(second.getId()));
    }

    @Test
    void returnsOnlyOwnActiveContextsAndDoesNotCombineOrganizations() {
        var orgA = organization("Udruženje A", OrganizationType.ASSOCIATION);
        var orgB = organization("Udruženje B", OrganizationType.ASSOCIATION);
        var mine = user("issuer-a", "same-subject");
        var otherIssuer = user("issuer-b", "same-subject");
        var active = new RoleAssignment(mine, Role.OPERATOR, orgA, NOW, null);
        var foreign = new RoleAssignment(otherIssuer, Role.OPERATOR, orgB, NOW, null);
        var expired = new RoleAssignment(mine, Role.WARDEN, orgB, NOW.minusSeconds(3600), NOW);
        var future = new RoleAssignment(mine, Role.ASSOCIATION_ADMIN, orgB, NOW.plusSeconds(1), null);
        var revoked = new RoleAssignment(mine, Role.WARDEN, orgA, NOW.minusSeconds(1), null);
        revoked.revoke(NOW);
        for (var grant : java.util.List.of(active, foreign, expired, future, revoked)) entityManager.persist(grant);
        entityManager.flush();
        entityManager.clear();

        var service = new AccessContextService(entityManager, Clock.fixed(NOW, ZoneOffset.UTC), false);
        assertThat(service.available("issuer-a", "same-subject")).singleElement().satisfies(context -> {
            assertThat(context.id()).isEqualTo(active.getId());
            assertThat(context.organization().id()).isEqualTo(orgA.getId());
        });
        assertThatThrownBy(() -> service.require("issuer-a", "same-subject", foreign.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.available("issuer-a", "unknown")).isEmpty();
    }

    @Test
    void deactivationAndArchivingRemoveContextsImmediately() {
        var organization = organization("Udruženje", OrganizationType.ASSOCIATION);
        var user = user("issuer", "subject");
        entityManager.persist(new RoleAssignment(user, Role.OPERATOR, organization, NOW, null));
        entityManager.flush();
        var service = new AccessContextService(entityManager, Clock.fixed(NOW, ZoneOffset.UTC), false);
        assertThat(service.available("issuer", "subject")).hasSize(1);
        organization.archive();
        entityManager.flush();
        assertThat(service.available("issuer", "subject")).isEmpty();
        var global = user("issuer", "admin");
        entityManager.persist(new RoleAssignment(global, Role.SYSTEM_ADMIN, null, NOW, null));
        entityManager.flush();
        assertThat(service.available("issuer", "admin")).hasSize(1);
        global.deactivate();
        entityManager.flush();
        assertThat(service.available("issuer", "admin")).isEmpty();
    }

    @Test
    void foreignKeyPreventsDeletingAnOrganizationReferencedByRole() {
        var organization = organization("Udruženje", OrganizationType.ASSOCIATION);
        var user = user("issuer", "subject");
        entityManager.persist(new RoleAssignment(user, Role.OPERATOR, organization, NOW, null));
        entityManager.flush();
        assertThatThrownBy(() -> entityManager.createNativeQuery("DELETE FROM organizations.organization WHERE id = :id")
                .setParameter("id", organization.getId()).executeUpdate()).isInstanceOf(PersistenceException.class);
    }

    @Test
    void rejectsInvalidHierarchyRoleScopeAndEmptyPeriods() {
        var association = new Organization("Udruženje", OrganizationType.ASSOCIATION, null, null);
        var ministry = new Organization("Ministarstvo", OrganizationType.MINISTRY, null, null);
        var user = new ApplicationUser("issuer", "subject");
        assertThatThrownBy(() -> new OrganizationAffiliation(association, ministry, NOW, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrganizationAffiliation(association, association, NOW, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RoleAssignment(user, Role.SYSTEM_ADMIN, association, NOW, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RoleAssignment(user, Role.OPERATOR, null, NOW, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RoleAssignment(user, Role.OPERATOR, association, NOW, NOW)).isInstanceOf(IllegalArgumentException.class);
        var role = new RoleAssignment(user, Role.OPERATOR, association, NOW, NOW.plusSeconds(1));
        assertThat(role.isActiveAt(NOW)).isTrue();
        assertThat(role.isActiveAt(NOW.plusSeconds(1))).isFalse();
    }

    @Test
    void optimisticVersionRejectsStaleOrganizationChanges() {
        var organization = organization("Izvorni naziv", OrganizationType.ASSOCIATION);
        entityManager.flush();
        entityManager.detach(organization);
        entityManager.createQuery("update Organization o set o.name = 'Noviji naziv', o.version = o.version + 1 where o.id = :id")
                .setParameter("id", organization.getId()).executeUpdate();
        organization.updateContact("Zastarjela izmjena", null, null);
        assertThatThrownBy(() -> entityManager.merge(organization)).isInstanceOf(jakarta.persistence.OptimisticLockException.class);
    }

    private Organization organization(String name, OrganizationType type) {
        var organization = new Organization(name, type, null, null);
        entityManager.persist(organization);
        return organization;
    }

    private ApplicationUser user(String issuer, String subject) {
        var user = new ApplicationUser(issuer, subject);
        entityManager.persist(user);
        return user;
    }
}
