package vn.org.thn.app.base.persistence.dialect;


import vn.org.thn.app.base.persistence.datasource.DatabaseType;

/** Maps a {@link DatabaseType} to its {@link SqlDialect} implementation. */
public final class DialectFactory {

    private DialectFactory() {
    }

    /** Builds the dialect for {@code type} (Postgres if {@code type} is null, matching the module's original default). */
    public static SqlDialect create(DatabaseType type) {
        if (type == null) {
            return new PostgreSqlDialect();
        }
        return switch (type) {
            case POSTGRESQL -> new PostgreSqlDialect();
            case MYSQL -> new MySqlDialect();
            case SQLSERVER -> new SqlServerDialect();
            case SQLITE -> new SqliteDialect();
            case ORACLE -> new OracleDialect();
        };
    }
}
