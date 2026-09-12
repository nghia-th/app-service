package vn.org.thn.app.base.persistence.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.config.DatabaseProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** {@link DatabaseProvider} for PostgreSQL - see {@link vn.org.thn.app.base.persistence.dialect.PostgreSqlDialect} for its SQL dialect. */
@Component
public class PostgreSqlProvider implements DatabaseProvider {

    @Value("${base.database.postgresql.host:localhost}")
    private String host;

    @Value("${base.database.postgresql.port:5432}")
    private int port;

    @Autowired
    private DatabaseProperties databaseProperties;

    @Override
    public DatabaseType type() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public String driverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String migrationFolder() {
        return "postgresql";
    }

    /** {@code jdbc:postgresql://<host>:<port>} - {@link #host}/{@link #port} are separate properties, not one URL string. */
    private String urlPrefix() {
        return "jdbc:postgresql://" + host + ":" + port;
    }

    /**
     * Builds the full JDBC URL from {@link #urlPrefix()} plus {@link DatabaseProperties#databaseName()}
     * - the database name itself is never hardcoded in a property, it always comes from
     * {@code base.database.db-prefix}/{@code base.database.db-name}.
     */
    @Override
    public String jdbcUrl() {
        return urlPrefix() + "/" + databaseProperties.databaseName();
    }

    /**
     * Connects to Postgres' always-present {@code postgres} maintenance database (same host/port/
     * credentials as {@link #jdbcUrl()}, just a different database name) to check whether the
     * target database exists, and issues {@code CREATE DATABASE} if it doesn't. Postgres has no
     * {@code CREATE DATABASE IF NOT EXISTS} syntax, and {@code CREATE DATABASE} cannot run against
     * the database it's creating - both are why this needs its own admin connection rather than a
     * single statement on the pooled {@link DataSource}.
     */
    @Override
    public void ensureDatabaseExists(DataSourceProperties dataSourceProperties) {
        String dbName = databaseProperties.databaseName();
        String adminUrl = urlPrefix() + "/postgres";

        try (Connection conn = DriverManager.getConnection(adminUrl, dataSourceProperties.getUsername(), dataSourceProperties.getPassword())) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                ps.setString(1, dbName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return;
                    }
                }
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE \"" + dbName + "\"");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot auto-create PostgreSQL database '" + dbName + "'", e);
        }
    }

    @Override
    public void initialize(DataSource dataSource) {
        // no engine-specific setup needed for Postgres
    }
}
