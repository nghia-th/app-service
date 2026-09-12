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

/** {@link DatabaseProvider} for Microsoft SQL Server - see {@link vn.org.thn.app.base.persistence.dialect.SqlServerDialect} for its SQL dialect. */
@Component
public class SqlServerProvider implements DatabaseProvider {

    @Value("${base.database.sqlserver.host:localhost}")
    private String host;

    @Value("${base.database.sqlserver.port:1433}")
    private int port;

    @Autowired
    private DatabaseProperties databaseProperties;

    @Override
    public DatabaseType type() {
        return DatabaseType.SQLSERVER;
    }

    @Override
    public String driverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String migrationFolder() {
        return "sqlserver";
    }

    /** {@code jdbc:sqlserver://<host>:<port>;encrypt=false} - {@link #host}/{@link #port} are separate properties, not one URL string. */
    private String urlPrefix() {
        return "jdbc:sqlserver://" + host + ":" + port + ";encrypt=false";
    }

    /**
     * Builds the full JDBC URL from {@link #urlPrefix()} plus {@code ;databaseName=} +
     * {@link DatabaseProperties#databaseName()} - the database name itself is never hardcoded in a
     * property, it always comes from {@code base.database.db-prefix}/{@code base.database.db-name}.
     */
    @Override
    public String jdbcUrl() {
        return urlPrefix() + ";databaseName=" + databaseProperties.databaseName();
    }

    /**
     * Connects to SQL Server's always-present {@code master} database (same server/port/credentials
     * as {@link #jdbcUrl()}, {@code databaseName} pointed at {@code master} instead) to check
     * {@code sys.databases} for the target database and issues {@code CREATE DATABASE} if it's
     * missing - the same two-step shape as {@link PostgreSqlProvider#ensureDatabaseExists} (SQL
     * Server has no single-statement {@code IF NOT EXISTS} form for {@code CREATE DATABASE} either,
     * and it can't be created from a connection already pointed at it).
     */
    @Override
    public void ensureDatabaseExists(DataSourceProperties dataSourceProperties) {
        String dbName = databaseProperties.databaseName();
        String adminUrl = urlPrefix() + ";databaseName=master";

        try (Connection conn = DriverManager.getConnection(adminUrl, dataSourceProperties.getUsername(), dataSourceProperties.getPassword())) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM sys.databases WHERE name = ?")) {
                ps.setString(1, dbName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return;
                    }
                }
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE [" + dbName + "]");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot auto-create SQL Server database '" + dbName + "'", e);
        }
    }

    @Override
    public void initialize(DataSource dataSource) {
        // no engine-specific setup needed for SQL Server
    }
}
