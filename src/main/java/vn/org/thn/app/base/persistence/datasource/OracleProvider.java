package vn.org.thn.app.base.persistence.datasource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/** Targets Oracle 12c+ (see {@link vn.org.thn.app.base.persistence.dialect.OracleDialect}). */
@Component
public class OracleProvider implements DatabaseProvider {

    @Value("${base.database.oracle.jdbc-url:jdbc:oracle:thin:@//localhost:1521/XEPDB1}")
    private String jdbcUrl;

    @Override
    public DatabaseType type() {
        return DatabaseType.ORACLE;
    }

    @Override
    public String driverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public String migrationFolder() {
        return "oracle";
    }

    @Override
    public String jdbcUrl() {
        return jdbcUrl;
    }

    /** No engine-specific setup needed for Oracle. */
    @Override
    public void initialize(DataSource dataSource) {
        // no engine-specific setup needed for Oracle
    }
}
