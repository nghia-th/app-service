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

    /** Plain INSERT - the generated identity value is retrieved in a second query via LAST_INSERT_ID() within the same transaction. */
    @Override
    public String buildInsertReturning(String table, String columns, String params, String identityColumn) {
        return "INSERT INTO " + table + " (" + columns + ") VALUES (" + params + ")";
    }

    @Override
    public boolean singleStatementReturning() {
        return false;
    }

    @Override
    public String buildIdentitySelect(String table, String identityColumn) {
        return "SELECT LAST_INSERT_ID();";
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
