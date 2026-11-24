package ba.ribolov.access.internal;

import ba.ribolov.access.internal.application.AccessContextService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${IT_DATABASE_URL}",
        "spring.datasource.username=${IT_DATABASE_USERNAME}",
        "spring.datasource.password=${IT_DATABASE_PASSWORD}",
        "spring.flyway.enabled=true",
        "spring.flyway.url=${IT_DATABASE_URL}",
        "spring.flyway.user=${IT_MIGRATION_USERNAME}",
        "spring.flyway.password=${IT_MIGRATION_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "ribolov.database.identity-context=true"
})
@ActiveProfiles("test")
class PostgresAccessIT {
    private static final String ISSUER = "https://identity.example.test";
    @Autowired AccessContextService contexts;
    @Autowired DataSource dataSource;
    private Connection owner;
    private UUID orgA, orgB, userA, userB, grantA, grantB;
    private String subjectA, subjectB;

    @BeforeEach
    void fixtures() throws Exception {
        owner = DriverManager.getConnection(System.getenv("IT_DATABASE_URL"),
                System.getenv("IT_MIGRATION_USERNAME"), System.getenv("IT_MIGRATION_PASSWORD"));
        orgA = UUID.randomUUID(); orgB = UUID.randomUUID();
        userA = UUID.randomUUID(); userB = UUID.randomUUID();
        grantA = UUID.randomUUID(); grantB = UUID.randomUUID();
        subjectA = userA.toString(); subjectB = userB.toString();
        insertOrganization(orgA, "A", "ASSOCIATION");
        insertOrganization(orgB, "B", "ASSOCIATION");
        insertUser(userA, subjectA);
        insertUser(userB, subjectB);
        insertGrant(grantA, userA, orgA);
        insertGrant(grantB, userB, orgB);
    }

    @AfterEach
    void clean() throws Exception {
        if (owner == null) return;
        try {
            execute("DELETE FROM access.role_assignment WHERE user_id IN (?, ?)", userA, userB);
            execute("DELETE FROM access.application_user WHERE id IN (?, ?)", userA, userB);
            execute("DELETE FROM organizations.organization_affiliation WHERE child_id IN (?, ?) OR parent_id IN (?, ?)", orgA, orgB, orgA, orgB);
            execute("DELETE FROM organizations.organization WHERE id IN (?, ?)", orgA, orgB);
        } finally { owner.close(); }
    }

    @Test
    void realJpaQueriesRespectRlsAndTransactionIdentityDoesNotLeak() throws Exception {
        assertThat(runtimeCount("access.role_assignment")).isZero();
        assertThat(contexts.available(ISSUER, subjectA)).singleElement().satisfies(context -> {
            assertThat(context.id()).isEqualTo(grantA);
            assertThat(context.organization().id()).isEqualTo(orgA);
        });
        assertThatThrownBy(() -> contexts.require(ISSUER, subjectA, grantB)).isInstanceOf(AccessDeniedException.class);
        assertThat(contexts.available("https://other-issuer.example.test", subjectA)).isEmpty();
        assertThat(contexts.available(ISSUER, subjectB)).singleElement().satisfies(context -> assertThat(context.id()).isEqualTo(grantB));
        assertThat(runtimeCount("access.application_user")).isZero();
        assertThat(runtimeCount("access.role_assignment")).isZero();
        assertThat(runtimeCount("organizations.organization")).isZero();
        try (var runtime = dataSource.getConnection(); var statement = runtime.prepareStatement(
                "INSERT INTO organizations.organization(id, name, type, status) VALUES (?, 'Forbidden', 'MINISTRY', 'ACTIVE')")) {
            statement.setObject(1, UUID.randomUUID());
            assertThatThrownBy(statement::executeUpdate).isInstanceOf(SQLException.class)
                    .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("42501"));
        }
    }

    @Test
    void revocationAndUserDeactivationAreEffectiveOnNextRequest() throws Exception {
        assertThat(contexts.available(ISSUER, subjectA)).hasSize(1);
        execute("UPDATE access.role_assignment SET revoked_at = CURRENT_TIMESTAMP WHERE id = ?", grantA);
        assertThat(contexts.available(ISSUER, subjectA)).isEmpty();
        assertThat(contexts.available(ISSUER, subjectB)).hasSize(1);
        execute("UPDATE access.application_user SET active = false WHERE id = ?", userB);
        assertThat(contexts.available(ISSUER, subjectB)).isEmpty();
    }

    @Test
    void databaseRejectsOverlappingRolesAndDanglingForeignKeys() throws Exception {
        assertSqlState("23P01", () -> insertGrant(UUID.randomUUID(), userA, orgA));
        assertSqlState("23503", () -> execute(
                "UPDATE access.role_assignment SET organization_id = ? WHERE id = ?", UUID.randomUUID(), grantA));
        assertSqlState("23503", () -> execute("DELETE FROM organizations.organization WHERE id = ?", orgA));
        execute("UPDATE access.role_assignment SET valid_until = CURRENT_TIMESTAMP WHERE id = ?", grantA);
        assertThat(contexts.available(ISSUER, subjectA)).isEmpty();
    }

    @Test
    void databasePreservesNonOverlappingOrganizationHistory() throws Exception {
        // Remove role references so the fixture organizations may serve as federation parents.
        execute("DELETE FROM access.role_assignment WHERE id IN (?, ?)", grantA, grantB);
        execute("UPDATE organizations.organization SET type = 'FEDERATION' WHERE id IN (?, ?)", orgA, orgB);
        UUID child = UUID.randomUUID();
        insertOrganization(child, "Child", "ASSOCIATION");
        try {
            execute("INSERT INTO organizations.organization_affiliation(id, child_id, parent_id, valid_from, valid_until) VALUES (?, ?, ?, '2025-01-01T00:00:00Z', '2026-01-01T00:00:00Z')",
                    UUID.randomUUID(), child, orgA);
            execute("INSERT INTO organizations.organization_affiliation(id, child_id, parent_id, valid_from) VALUES (?, ?, ?, '2026-01-01T00:00:00Z')",
                    UUID.randomUUID(), child, orgB);
            assertSqlState("23P01", () -> execute(
                    "INSERT INTO organizations.organization_affiliation(id, child_id, parent_id, valid_from) VALUES (?, ?, ?, '2025-06-01T00:00:00Z')",
                    UUID.randomUUID(), child, orgB));
            assertSqlState("23514", () -> execute(
                    "INSERT INTO organizations.organization_affiliation(id, child_id, parent_id, valid_from) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                    UUID.randomUUID(), orgA, child));
            try (var statement = owner.prepareStatement("SELECT count(*) FROM organizations.organization_affiliation WHERE child_id = ?")) {
                statement.setObject(1, child);
                try (var result = statement.executeQuery()) { result.next(); assertThat(result.getInt(1)).isEqualTo(2); }
            }
        } finally {
            execute("DELETE FROM organizations.organization_affiliation WHERE child_id = ?", child);
            execute("DELETE FROM organizations.organization WHERE id = ?", child);
        }
    }

    private int runtimeCount(String table) throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT count(*) FROM " + table)) {
            result.next(); return result.getInt(1);
        }
    }

    private void insertOrganization(UUID id, String name, String type) throws SQLException {
        execute("INSERT INTO organizations.organization(id, name, type, status) VALUES (?, ?, ?, 'ACTIVE')", id, name, type);
    }

    private void insertUser(UUID id, String subject) throws SQLException {
        execute("INSERT INTO access.application_user(id, issuer, subject, active) VALUES (?, ?, ?, true)", id, ISSUER, subject);
    }

    private void insertGrant(UUID id, UUID user, UUID organization) throws SQLException {
        execute("INSERT INTO access.role_assignment(id, user_id, organization_id, role, valid_from) VALUES (?, ?, ?, 'OPERATOR', CURRENT_TIMESTAMP - interval '1 hour')", id, user, organization);
    }

    private void execute(String sql, Object... values) throws SQLException {
        try (var statement = owner.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            statement.executeUpdate();
        }
    }

    private void assertSqlState(String state, SqlAction action) {
        assertThatThrownBy(action::run).isInstanceOf(SQLException.class)
                .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo(state));
    }
    @FunctionalInterface private interface SqlAction { void run() throws SQLException; }
}
