package ba.ribolov;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresMigrationIT {
    private static final List<String> MODULES = List.of("access", "people", "membership", "waters",
            "billing", "permits", "fieldwork", "reporting", "documents", "notifications", "audit");

    @Test
    void migrationsAreRepeatableAndRuntimeAccountCannotChangeSchema() throws Exception {
        String url = required("IT_DATABASE_URL");
        String owner = required("IT_MIGRATION_USERNAME");
        String ownerPassword = required("IT_MIGRATION_PASSWORD");
        var flyway = Flyway.configure().dataSource(url, owner, ownerPassword)
                .defaultSchema("platform").createSchemas(true).load();
        flyway.migrate();
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (Connection runtime = DriverManager.getConnection(url, required("IT_DATABASE_USERNAME"),
                required("IT_DATABASE_PASSWORD"));
             var query = runtime.createStatement()) {
            try (var rows = query.executeQuery("SELECT nspname FROM pg_namespace")) {
                var schemas = new ArrayList<String>();
                while (rows.next()) schemas.add(rows.getString(1));
                assertThat(schemas).containsAll(MODULES);
            }
            try (var rows = query.executeQuery(
                    "SELECT rolsuper, rolcreatedb, rolcreaterole, rolbypassrls FROM pg_roles WHERE rolname = current_user")) {
                assertThat(rows.next()).isTrue();
                for (int column = 1; column <= 4; column++) assertThat(rows.getBoolean(column)).isFalse();
            }
            for (String schema : MODULES) {
                try (var rights = runtime.prepareStatement(
                        "SELECT has_schema_privilege(current_user, ?, 'USAGE'), has_schema_privilege(current_user, ?, 'CREATE')")) {
                    rights.setString(1, schema);
                    rights.setString(2, schema);
                    try (var rows = rights.executeQuery()) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getBoolean(1)).isTrue();
                        assertThat(rows.getBoolean(2)).isFalse();
                    }
                }
            }
            // Roll back even if an incorrect grant lets this statement succeed.
            runtime.setAutoCommit(false);
            try {
                assertThatThrownBy(() -> query.execute("CREATE TABLE people.forbidden_ddl (id integer)"))
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("42501"));
            } finally {
                runtime.rollback();
            }
        }
        // Check default privileges on a future domain table without persisting test data.
        try (Connection migration = DriverManager.getConnection(url, owner, ownerPassword)) {
            migration.setAutoCommit(false);
            try (var query = migration.createStatement()) {
                query.execute("CREATE TABLE people.privilege_probe (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY)");
                try (var rows = query.executeQuery(
                        "SELECT has_table_privilege('ribolov_app', 'people.privilege_probe', 'SELECT') AND has_table_privilege('ribolov_app', 'people.privilege_probe', 'INSERT') AND has_table_privilege('ribolov_app', 'people.privilege_probe', 'UPDATE') AND has_table_privilege('ribolov_app', 'people.privilege_probe', 'DELETE')")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getBoolean(1)).isTrue();
                }
            } finally {
                migration.rollback();
            }
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Set " + name + " for PostgreSQL tests");
        return value;
    }
}
