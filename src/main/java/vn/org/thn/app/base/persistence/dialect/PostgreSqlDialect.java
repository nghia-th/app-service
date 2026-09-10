package vn.org.thn.app.base.persistence.dialect;

/** {@link SqlDialect} for PostgreSQL - the module's original/default engine. */
public class PostgreSqlDialect implements SqlDialect {

    @Override
    public String getName() {
        return "postgresql";
    }

    @Override
    public String buildPagingSql(String sql, Integer limit, Integer offset) {
        String limitPart = limit != null ? " LIMIT " + limit : "";
        String offsetPart = offset != null ? " OFFSET " + offset : "";
        return sql + limitPart + offsetPart;
    }

    /** Postgres's {@code RETURNING} clause hands back the new identity value from the same INSERT statement, one round trip. */
    @Override
    public String buildInsertReturning(String table, String columns, String params, String identityColumn) {
        if (identityColumn != null) {
            return "INSERT INTO " + table + " (" + columns + ") VALUES (" + params + ") RETURNING " + identityColumn;
        }
        return "INSERT INTO " + table + " (" + columns + ") VALUES (" + params + ")";
    }

    @Override
    public String buildIdentityInsert(String table, String insertSql) {
        return insertSql;
    }

    @Override
    public boolean supportBatchInsert() {
        return true;
    }

    @Override
    public boolean supportIdentity() {
        return true;
    }

    /** After a manual-id insert into an identity column, Postgres's own sequence must be fast-forwarded past the highest id in use, or the next auto-generated insert will collide. */
    @Override
    public String buildSequenceSync(String table, String identityColumn) {
        return "SELECT setval(pg_get_serial_sequence('" + table + "', '" + identityColumn + "'), "
                + "(SELECT MAX(" + identityColumn + ") FROM " + table + "))";
    }

    @Override
    public int maxBatchRows(int columnCount) {
        return 5000;
    }
}
