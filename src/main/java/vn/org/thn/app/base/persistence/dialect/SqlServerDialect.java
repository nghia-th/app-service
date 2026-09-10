package vn.org.thn.app.base.persistence.dialect;

/** {@link SqlDialect} for Microsoft SQL Server. */
public class SqlServerDialect implements SqlDialect {

    @Override
    public String getName() {
        return "sqlserver";
    }

    /** SQL Server's {@code OFFSET ... FETCH NEXT} paging requires an {@code ORDER BY}; one is appended (by column 1) when the caller's SQL doesn't already have one. */
    @Override
    public String buildPagingSql(String sql, Integer limit, Integer offset) {
        if (limit == null && offset == null) {
            return sql;
        }
        long off = offset != null ? offset : 0;
        long lim = limit != null ? limit : Long.MAX_VALUE;
        boolean hasOrder = sql.toLowerCase().contains("order by");
        String finalSql = hasOrder ? sql : sql + " ORDER BY 1";
        return finalSql + " OFFSET " + off + " ROWS FETCH NEXT " + lim + " ROWS ONLY";
    }

    /** {@code SCOPE_IDENTITY()} reads back the identity value from the INSERT run just before it in the same scope/batch. */
    @Override
    public String buildInsertReturning(String table, String columns, String params, String identityColumn) {
        return "INSERT INTO " + table + " (" + columns + ")\n"
                + "VALUES (" + params + ");\n\n"
                + "SELECT SCOPE_IDENTITY();";
    }

    /** Wraps a manual-id insert in {@code SET IDENTITY_INSERT ... ON/OFF}, required by SQL Server to write an explicit value into an identity column. */
    @Override
    public String buildIdentityInsert(String table, String insertSql) {
        return "SET IDENTITY_INSERT " + table + " ON;\n"
                + insertSql + ";\n"
                + "SET IDENTITY_INSERT " + table + " OFF;";
    }

    @Override
    public boolean supportBatchInsert() {
        return true;
    }

    @Override
    public boolean supportIdentity() {
        return true;
    }

    @Override
    public String buildSequenceSync(String table, String identityColumn) {
        return null;
    }

    /** SQL Server caps a single statement at 2100 bound parameters; this leaves a small margin and floors/ceilings the result to a sane row-count range. */
    @Override
    public int maxBatchRows(int columnCount) {
        int rows = 2100 / Math.max(columnCount, 1);
        return Math.min(Math.max(rows, 1), 500);
    }
}
