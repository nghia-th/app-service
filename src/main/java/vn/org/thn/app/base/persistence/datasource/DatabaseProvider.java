package vn.org.thn.app.base.persistence.datasource;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;

import javax.sql.DataSource;

/** One implementation per supported {@link DatabaseType}, auto-discovered as a Spring bean. */
public interface DatabaseProvider {

    /** Which engine this provider is for. */
    DatabaseType type();

    /** Fully-qualified JDBC driver class name for this engine. */
    String driverClassName();

    /** Sub-folder name under database/ that holds this engine's Flyway migration scripts. */
    String migrationFolder();

    /** Default JDBC connection URL, overridable via application properties. */
    String jdbcUrl();

    /**
     * Creates the target database itself if it doesn't exist yet, called once before the pooled
     * {@link DataSource} in {@link vn.org.thn.app.base.config.DataSourceConfig} is built
     * against it (a {@code DataSource} pointed at a nonexistent database fails immediately, so this
     * has to run against a separate admin connection first). No-op by default. Implemented for
     * Postgres, MySQL and SQL Server (see each provider's own override); SQLite doesn't need an
     * override since its driver already creates the file automatically. Deliberately left as a
     * no-op for Oracle - Oracle has no lightweight equivalent of {@code CREATE DATABASE} (creating a
     * new pluggable database needs a separate SYSDBA connection to the CDB root, its own admin
     * credentials and datafile paths, none of which this project captures yet), and the default
     * {@code XEPDB1} target is the pluggable database that already ships with Oracle XE, so the
     * common case needs no creation anyway. Oracle still fails the same way as before if a
     * non-default target database is missing.
     */
    default void ensureDatabaseExists(DataSourceProperties dataSourceProperties) {
        // no-op by default
    }

    /**
     * Guards against a malformed database name reaching a DDL statement built by string
     * concatenation (e.g. {@code CREATE DATABASE "} + dbName + {@code "}) in an
     * {@code ensureDatabaseExists} override - see the Low finding on {@code ensureDatabaseExists}
     * (2026-09-12 review). {@code dbName} always comes from internal config
     * ({@code base.database.db-prefix}/{@code base.database.db-name}), never end-user input, so
     * this is defense in depth rather than a response to a known exploitable path.
     */
    default String validateDatabaseName(String dbName) {
        if (dbName == null || !dbName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException("Invalid database name '" + dbName
                    + "' - only letters, digits and underscores are allowed (check base.database.db-prefix/db-name)");
        }
        return dbName;
    }

    /** Engine-specific one-time setup run against a fresh DataSource (e.g. SQLite PRAGMAs). */
    void initialize(DataSource dataSource);
}
