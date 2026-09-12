package vn.org.thn.app.base.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import vn.org.thn.app.base.persistence.datasource.DatabaseType;
import vn.org.thn.app.base.persistence.logging.SqlLogLevel;

/**
 * base.database.type            -- which DatabaseProvider/SqlDialect to use (default: SQLITE)
 * base.database.sql-log         -- OFF / BASIC / FULL (default: OFF)
 * base.database.pool.*          -- HikariCP pool sizing (see {@link Pool})
 * base.database.db-prefix       -- optional prefix combined with db-name, see {@link #databaseName()}
 * base.database.db-name         -- database name each engine's JDBC URL is built from (default: "app")
 * <p>
 * Every field is also settable through the matching environment variable via Spring Boot's
 * standard relaxed binding, e.g. {@code BASE_DATABASE_TYPE=MYSQL} <=> {@code base.database.type=MYSQL}.
 */
@ConfigurationProperties(prefix = "base.database")
public class DatabaseProperties {

    private DatabaseType type = DatabaseType.SQLITE;
    private SqlLogLevel sqlLog = SqlLogLevel.OFF;
    private Pool pool = new Pool();
    private String dbPrefix = "";
    private String dbName = "app";

    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
        this.type = type;
    }

    public SqlLogLevel getSqlLog() {
        return sqlLog;
    }

    public void setSqlLog(SqlLogLevel sqlLog) {
        this.sqlLog = sqlLog;
    }

    public Pool getPool() {
        return pool;
    }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public String getDbPrefix() {
        return dbPrefix;
    }

    public void setDbPrefix(String dbPrefix) {
        this.dbPrefix = dbPrefix;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    /**
     * The actual database/schema name every {@link vn.org.thn.app.base.persistence.datasource.DatabaseProvider}
     * builds its JDBC URL from (Postgres/MySQL/SQL Server - not Oracle, which keeps its own
     * self-contained {@code jdbc-url} property; see {@code OracleProvider}'s class doc) and that
     * {@link vn.org.thn.app.base.persistence.datasource.DatabaseProvider#ensureDatabaseExists} creates if missing:
     * {@link #dbPrefix} + {@code "_"} + {@link #dbName} when a prefix is set (e.g. {@code "dev"} +
     * {@code "example"} -> {@code "dev_example"}), or just {@link #dbName} when {@link #dbPrefix} is
     * blank (the default) - so the name is never hardcoded inside a provider's JDBC URL property.
     */
    public String databaseName() {
        if (dbPrefix == null || dbPrefix.isBlank()) {
            return dbName;
        }
        return dbPrefix + "_" + dbName;
    }

    /**
     * HikariCP pool sizing and timeouts, overridable per service/environment via
     * {@code base.database.pool.*} without touching code. {@link #maxPoolSize}/{@link #minIdle}
     * default to this module's original hardcoded values (a small footprint, suitable for a
     * low-traffic service) - raise them for a service under heavier concurrent load.
     * {@link #connectionTimeoutMs}/{@link #maxLifetimeMs}/{@link #leakDetectionThresholdMs} default
     * to HikariCP's own library defaults (previously left unset here, so a pool leak or a stuck
     * connection acquisition had no explicit budget to surface against - see Low finding, 2026-09-12
     * review) - set explicitly so they show up as real, overridable config instead of implicit
     * library defaults, and so {@code leak-detection-threshold-ms} can be turned on in prod without
     * a code change.
     */
    public static class Pool {

        private int maxPoolSize = 5;
        private int minIdle = 1;
        private long connectionTimeoutMs = 30_000L;
        private long maxLifetimeMs = 1_800_000L;
        private long leakDetectionThresholdMs = 0L;

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        public long getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }

        public void setConnectionTimeoutMs(long connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }

        public long getMaxLifetimeMs() {
            return maxLifetimeMs;
        }

        public void setMaxLifetimeMs(long maxLifetimeMs) {
            this.maxLifetimeMs = maxLifetimeMs;
        }

        /** 0 (the default here, same as HikariCP's own default) disables leak detection; a positive value logs a warning for any connection held longer than this without being closed. */
        public long getLeakDetectionThresholdMs() {
            return leakDetectionThresholdMs;
        }

        public void setLeakDetectionThresholdMs(long leakDetectionThresholdMs) {
            this.leakDetectionThresholdMs = leakDetectionThresholdMs;
        }
    }
}
