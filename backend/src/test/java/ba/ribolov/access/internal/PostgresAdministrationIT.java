package ba.ribolov.access.internal;

import ba.ribolov.access.api.Role;
import ba.ribolov.access.internal.administration.*;
import ba.ribolov.organizations.api.OrganizationManagement;
import ba.ribolov.organizations.model.OrganizationType;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${IT_DATABASE_URL}", "spring.datasource.username=${IT_DATABASE_USERNAME}",
        "spring.datasource.password=${IT_DATABASE_PASSWORD}", "spring.flyway.enabled=true",
        "spring.flyway.url=${IT_DATABASE_URL}", "spring.flyway.user=${IT_MIGRATION_USERNAME}",
        "spring.flyway.password=${IT_MIGRATION_PASSWORD}", "spring.jpa.hibernate.ddl-auto=validate",
        "ribolov.database.identity-context=true"
})
@ActiveProfiles("test")
class PostgresAdministrationIT {
    private static final String ISSUER = "https://identity.example.test";
    @Autowired AdministrationService service;
    @Autowired DataSource runtime;
    private Connection owner;
    private final List<UUID> users = new ArrayList<>(), organizations = new ArrayList<>();
    private UUID sys, admin, target, other, a, b, sysContext, aContext, bContext;
    @BeforeEach
    void fixtures() throws Exception {
        owner = DriverManager.getConnection(System.getenv("IT_DATABASE_URL"), System.getenv("IT_MIGRATION_USERNAME"), System.getenv("IT_MIGRATION_PASSWORD"));
        sys = user("System"); admin = user("Admin"); target = user("Target"); other = user("Other");
        a = organization("A"); b = organization("B");
        sysContext = role(sys, null, "SYSTEM_ADMIN");
        aContext = role(admin, a, "ASSOCIATION_ADMIN");
        bContext = role(admin, b, "ASSOCIATION_ADMIN");
        role(other, b, "OPERATOR");
    }
    @AfterEach
    void cleanup() throws Exception {
        if (owner == null) return;
        try {
            for (UUID id : users) execute("DELETE FROM access.administration_event WHERE actor_id = ? OR target_user_id = ?", id, id);
            for (UUID id : users) execute("DELETE FROM access.role_assignment WHERE user_id = ?", id);
            for (UUID id : organizations) execute("DELETE FROM organizations.organization_affiliation WHERE child_id = ? OR parent_id = ?", id, id);
            for (UUID id : organizations) execute("DELETE FROM organizations.organization WHERE id = ?", id);
            for (UUID id : users) execute("DELETE FROM access.application_user WHERE id = ?", id);
        } finally { owner.close(); users.clear(); organizations.clear(); }
    }

    @Test
    void applicationWritesHierarchyAndPreservesHistoryWithRuntimeAccount() {
        var ministry = service.createOrganization(caller(sys), sysContext,
                new OrganizationManagement.Create("Ministarstvo", OrganizationType.MINISTRY, null, null, null), "Osnivanje");
        organizations.add(ministry.id());
        var first = service.createOrganization(caller(sys), sysContext,
                new OrganizationManagement.Create("Prvi savez", OrganizationType.FEDERATION, null, null, ministry.id()), "Osnivanje");
        organizations.add(first.id());
        var second = service.createOrganization(caller(sys), sysContext,
                new OrganizationManagement.Create("Drugi savez", OrganizationType.FEDERATION, null, null, ministry.id()), "Osnivanje");
        organizations.add(second.id());
        var child = service.createOrganization(caller(sys), sysContext,
                new OrganizationManagement.Create("Udruženje", OrganizationType.ASSOCIATION, null, null, first.id()), "Osnivanje");
        organizations.add(child.id());
        var changed = service.changeParent(caller(sys), sysContext, child.id(),
                new OrganizationManagement.ChangeParent(child.version(), second.id(), null), "Promjena nadležnosti");
        assertThat(changed.parent().id()).isEqualTo(second.id());
        assertThat(changed.version()).isGreaterThan(child.version());
        assertThat(service.history(caller(sys), sysContext, child.id())).hasSize(2);
        assertThat(service.events(caller(sys), sysContext, 0).items()).hasSize(5);
    }

    @Test
    void associationAdminCanGrantOnlyInsideSelectedOrganizationEvenWithTwoAdminRoles() {
        assertThat(service.organizations(caller(admin), aContext, "", 0).items()).extracting(OrganizationManagement.View::id).containsExactly(a);
        assertThat(service.users(caller(admin), aContext, "", 0).items()).noneMatch(u -> u.id().equals(other) || u.id().equals(target));
        var assignment = service.grant(caller(admin), aContext, new AdminViews.Grant(target, a, Role.OPERATOR, null, null, "Dodjela"));
        assertThat(assignment.user().id()).isEqualTo(target);
        assertThat(service.users(caller(admin), aContext, "", 0).items()).anyMatch(u -> u.id().equals(target));
        assertThatThrownBy(() -> service.grant(caller(admin), aContext,
                new AdminViews.Grant(target, b, Role.OPERATOR, null, null, "Pogrešan opseg")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> service.grant(caller(admin), aContext,
                new AdminViews.Grant(target, a, Role.ASSOCIATION_ADMIN, null, null, "Eskalacija")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(service.revoke(caller(admin), aContext, assignment.id(),
                new AdminViews.Revoke(assignment.version(), "Opoziv")).revokedAt()).isNotNull();
        assertThat(service.events(caller(admin), aContext, 0).items()).hasSize(2);
        assertThat(service.events(caller(admin), bContext, 0).items()).isEmpty();
    }

    @Test
    void systemAdminConnectsUserAndManagesOnlyAdministrativeRoles() {
        var user = service.createUser(caller(sys), sysContext, new AdminViews.CreateUser("new-" + UUID.randomUUID(), "Novi korisnik", "Povezivanje"));
        users.add(user.id());
        var assignment = service.grant(caller(sys), sysContext, new AdminViews.Grant(user.id(), a, Role.ASSOCIATION_ADMIN, null, null, "Imenovanje"));
        assertThat(assignment.organization().id()).isEqualTo(a);
        assertThatThrownBy(() -> service.grant(caller(sys), sysContext, new AdminViews.Grant(user.id(), a, Role.OPERATOR, null, null, "Neovlašteno")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(service.updateUser(caller(sys), sysContext, user.id(),
                new AdminViews.UpdateUser(user.version(), "Ispravljeno ime", false, "Deaktiviranje")).active()).isFalse();
    }

    @Test
    void failedAuditValidationRollsBackTheBusinessMutation() throws Exception {
        String name = "rollback-" + UUID.randomUUID();
        assertThatThrownBy(() -> service.createOrganization(caller(sys), sysContext,
                new OrganizationManagement.Create(name, OrganizationType.MINISTRY, null, null, null), " "))
                .isInstanceOf(IllegalArgumentException.class);
        try (var q = owner.prepareStatement("SELECT count(*) FROM organizations.organization WHERE name = ?")) {
            q.setString(1, name);
            try (var rows = q.executeQuery()) { rows.next(); assertThat(rows.getInt(1)).isZero(); }
        }
        assertThat(service.events(caller(sys), sysContext, 0).items()).isEmpty();
    }

    @Test
    void staleVersionDoesNotOverwriteDataOrAddAuditEvent() {
        var initial = service.organizations(caller(admin), aContext, "", 0).items().getFirst();
        var updated = service.updateOrganization(caller(admin), aContext, a,
                new OrganizationManagement.Update(initial.version(), "Novo A", null, null), "Prva izmjena");
        assertThat(updated.version()).isGreaterThan(initial.version());
        assertThatThrownBy(() -> service.updateOrganization(caller(admin), aContext, a,
                new OrganizationManagement.Update(initial.version(), "Zastarjelo", null, null), "Druga izmjena"))
                .isInstanceOf(jakarta.persistence.OptimisticLockException.class);
        assertThat(service.events(caller(admin), aContext, 0).items()).hasSize(1);
        assertThat(service.organizations(caller(admin), aContext, "", 0).items().getFirst().name()).isEqualTo("Novo A");
    }

    @Test
    void rowPoliciesProtectOtherOrganizationsImmutableColumnsAndAudit() throws Exception {
        service.updateOrganization(caller(admin), aContext, a, new OrganizationManagement.Update(0L, "Novo A", null, null), "Izmjena");
        try (var connection = runtime.getConnection()) {
            connection.setAutoCommit(false);
            identity(connection, admin, aContext, Instant.now());
            assertThat(count(connection, "SELECT count(*) FROM organizations.organization")).isEqualTo(1);
            assertThat(count(connection, "SELECT count(*) FROM access.role_assignment WHERE organization_id = '" + b + "'")).isZero();
            assertThat(count(connection, "SELECT count(*) FROM access.administration_event")).isEqualTo(1);
            connection.rollback();
            assertThat(count(connection, "SELECT count(*) FROM organizations.organization")).isZero();
            connection.rollback();
            identity(connection, admin, aContext, Instant.now());
            try (var q = connection.prepareStatement("INSERT INTO access.role_assignment(id,user_id,organization_id,role,valid_from) VALUES (?,?,?,'OPERATOR',CURRENT_TIMESTAMP)")) {
                q.setObject(1, UUID.randomUUID()); q.setObject(2, target); q.setObject(3, b);
                assertSqlState("42501", q::executeUpdate);
            }
            connection.rollback();
            identity(connection, admin, aContext, Instant.now());
            try (var q = connection.createStatement()) { assertSqlState("42501", () -> q.executeUpdate("DELETE FROM access.administration_event")); }
            connection.rollback();
            identity(connection, sys, sysContext, Instant.now());
            try (var q = connection.prepareStatement("UPDATE organizations.organization SET type = 'MINISTRY' WHERE id = ?")) {
                q.setObject(1, a); assertSqlState("42501", q::executeUpdate);
            }
            connection.rollback();
            identity(connection, sys, sysContext, Instant.now().minusSeconds(301));
            try (var q = connection.prepareStatement("INSERT INTO organizations.organization(id,name,type,status) VALUES (?,'Stale','MINISTRY','ACTIVE')")) {
                q.setObject(1, UUID.randomUUID()); assertSqlState("42501", q::executeUpdate);
            }
            connection.rollback();
        }
    }

    private void identity(Connection c, UUID user, UUID context, Instant authTime) throws SQLException {
        try (var q = c.prepareStatement("SELECT set_config('ribolov.issuer', ?, true), set_config('ribolov.subject', ?, true),"
                + " set_config('ribolov.assignment_id', ?, true), set_config('ribolov.acr', '2', true), set_config('ribolov.auth_time', ?, true)")) {
            q.setString(1, ISSUER); q.setString(2, user.toString()); q.setString(3, context.toString()); q.setString(4, Long.toString(authTime.getEpochSecond())); q.execute();
        }
    }
    private AdminCaller caller(UUID id) { return new AdminCaller(ISSUER, id.toString(), "2", Instant.now()); }
    private UUID user(String name) throws SQLException {
        UUID id = UUID.randomUUID(); users.add(id);
        execute("INSERT INTO access.application_user(id,issuer,subject,display_name,active) VALUES (?,?,?,?,true)", id, ISSUER, id.toString(), name); return id;
    }
    private UUID organization(String name) throws SQLException {
        UUID id = UUID.randomUUID(); organizations.add(id);
        execute("INSERT INTO organizations.organization(id,name,type,status) VALUES (?,?,'ASSOCIATION','ACTIVE')", id, name); return id;
    }
    private UUID role(UUID user, UUID organization, String role) throws SQLException {
        UUID id = UUID.randomUUID();
        execute("INSERT INTO access.role_assignment(id,user_id,organization_id,role,valid_from) VALUES (?,?,?,?,CURRENT_TIMESTAMP - interval '1 hour')", id, user, organization, role); return id;
    }
    private void execute(String sql, Object... values) throws SQLException {
        try (var q = owner.prepareStatement(sql)) { for (int i=0; i<values.length; i++) q.setObject(i+1, values[i]); q.executeUpdate(); }
    }
    private int count(Connection c, String sql) throws SQLException {
        try (var q = c.createStatement(); var rows = q.executeQuery(sql)) { rows.next(); return rows.getInt(1); }
    }
    private void assertSqlState(String state, SqlAction action) {
        assertThatThrownBy(action::run).isInstanceOf(SQLException.class).satisfies(e -> assertThat(((SQLException)e).getSQLState()).isEqualTo(state));
    }
    @FunctionalInterface private interface SqlAction { void run() throws SQLException; }
}
