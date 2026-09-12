package vn.org.thn.app.base.persistence.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.config.DatabaseProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * {@code allowMultiQueries=true} is required in {@link #CONNECTION_PARAMS} below - unlike
 * Postgres/SQL Server/SQLite, MySQL Connector/J rejects a multi-statement string (the "INSERT ...;
 * SELECT LAST_INSERT_ID();" that {@code MySqlDialect#buildInsertReturning} produces) unless that
 * connection property is set.
 */
@Component
public class MySqlProvider implements DatabaseProvider {

    private static final String CONNECTION_PARAMS = "?useSSL=false&serverTimezone=UTC&allowMultiQueries=true";

    @Value("${base.database.mysql.host:localhost}")
    private String host;

    @Value("${base.database.mysql.port:3306}")
    private int port;

    @Autowired
    private DatabaseProperties databaseProperties;

    @Override
    public DatabaseType type() {
        return DatabaseType.MYSQL;
    }

    @Override
    public String driverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String migrationFolder() {
        return "mysql";
    }

    /** {@code jdbc:mysql://<host>:<port>} - {@link #host}/{@link #port} are separate properties, not one URL string. */
    private String urlPrefix() {
        return "jdbc:mysql://" + host + ":" + port;
    }

    /**
     * Builds the full JDBC URL from {@link #urlPrefix()} plus {@link DatabaseProperties#databaseName()}
     * (and the required {@link #CONNECTION_PARAMS}) - the database name itself is never hardcoded
     * in a property, it always comes from {@code base.database.db-prefix}/{@code base.database.db-name}.
     */
    @Override
    public String jdbcUrl() {
        return urlPrefix() + "/" + databaseProperties.databaseName() + CONNECTION_PARAMS;
    }

    /**
     * Connects to the MySQL server without selecting a default schema (same host/port/credentials
     * as {@link #jdbcUrl()}, just no database name) and issues
     * {@code CREATE DATABASE IF NOT EXISTS}, which MySQL supports directly in one statement -
     * simpler than Postgres, which has no such single-statement form (see
     * {@link PostgreSqlProvider#ensureDatabaseExists}).
     */
    @Override
    public void ensureDatabaseExists(DataSourceProperties dataSourceProperties) {
        String dbName = validateDatabaseName(databaseProperties.databaseName());

        try (Connection conn = DriverManager.getConnection(urlPrefix(), dataSourceProperties.getUsername(), dataSourceProperties.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot auto-create MySQL database '" + dbName + "'", e);
        }
    }

    @Override
    public void initialize(DataSource dataSource) {
        // no engine-specific setup needed for MySQL
    }
}
