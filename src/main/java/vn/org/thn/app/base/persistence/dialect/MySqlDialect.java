package vn.org.thn.app.base.persistence.dialect;

/** {@link SqlDialect} for MySQL 8+. Requires {@code allowMultiQueries=true} on the JDBC URL (see {@link vn.org.thn.service.base.db.MySqlProvider}) since {@link #buildInsertReturning} produces a multi-statement string. */
public class MySqlDialect implements SqlDialect {

    @Override
    public String getName() {
        return "mysql";
    }

    @Override
    public int maxBatchRows(int columnCount) {
        return 2000;
    }

    @Override
    public String buildSequenceSync(String table, String identityColumn) {
        return null;
    }

    @Override
    public String buildPagingSql(String sql, Integer limit, Integer offset) {
        if (limit == null && offset == null) {
            return sql;
        }
        long off = offset != null ? offset : 0;
        long lim = limit != null ? limit : Long.MAX_VALUE;
        return sql + " LIMIT " + off + ", " + lim;
    }

    /** MySQL's session-scoped {@code LAST_INSERT_ID()} reads back the identity value from the INSERT run just before it in the same statement/connection. */
    @Override
    public String buildInsertReturning(String table, String columns, String params, String identityColumn) {
        return "INSERT INTO " + table + " (" + columns + ")\n"
                + "VALUES (" + params + ");\n"
                + "SELECT LAST_INSERT_ID();";
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
}
