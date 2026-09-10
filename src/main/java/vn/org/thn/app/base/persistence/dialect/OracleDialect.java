package vn.org.thn.app.base.persistence.dialect;

/**
 * Targets Oracle 12c and newer (ANSI OFFSET/FETCH paging, IDENTITY columns). Older Oracle
 * (ROWNUM-based paging, sequence + BEFORE INSERT trigger for ids) is not supported.
 * <p>
 * Unlike Postgres/MySQL/SQL Server/SQLite, plain JDBC has no way to combine "INSERT" and "read
 * back the new id" into one round trip for Oracle (its {@code RETURNING ... INTO} clause needs an
 * OUT-bound {@code CallableStatement}, which doesn't fit this module's generic "one raw SQL
 * string -&gt; one query" executor) - see {@link #singleStatementReturning()}: {@code false} here
 * means {@code InsertExecutor} runs {@link #buildInsertReturning} as a plain INSERT first, then
 * separately queries {@link #buildIdentitySelect}. That second query is a pragmatic
 * {@code SELECT MAX(id)} - not safe under concurrent inserts into the same table; fine for
 * low-concurrency/admin tables, but do not rely on it where writers race.
 */
public class OracleDialect implements SqlDialect {

    @Override
    public String getName() {
        return "oracle";
    }

    /** Appends ANSI {@code OFFSET ... ROWS [FETCH NEXT ... ROWS ONLY]} paging (Oracle 12c+ syntax; no ROWNUM fallback). */
    @Override
    public String buildPagingSql(String sql, Integer limit, Integer offset) {
        if (limit == null && offset == null) {
            return sql;
        }
        long off = offset != null ? offset : 0;
        StringBuilder result = new StringBuilder(sql).append(" OFFSET ").append(off).append(" ROWS");
        if (limit != null) {
            result.append(" FETCH NEXT ").append(limit).append(" ROWS ONLY");
        }
        return result.toString();
    }

    /** Plain INSERT with no RETURNING clause - see the class doc for why Oracle can't combine insert-and-read-id in one statement here. */
    @Override
    public String buildInsertReturning(String table, String columns, String params, String identityColumn) {
        return "INSERT INTO " + table + " (" + columns + ") VALUES (" + params + ")";
    }

    /** Always false for Oracle - see the class doc. */
    @Override
    public boolean singleStatementReturning() {
        return false;
    }

    /** Best-effort {@code SELECT MAX(id)} to recover the just-inserted id - see the class doc for its concurrency caveat. */
    @Override
    public String buildIdentitySelect(String table, String identityColumn) {
        return "SELECT MAX(" + identityColumn + ") AS " + identityColumn + " FROM " + table;
    }

    /** No wrapping needed - Oracle has no {@code IDENTITY_INSERT}-style toggle; returns {@code insertSql} unchanged. */
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

    /** Not needed for Oracle identity columns; always null. */
    @Override
    public String buildSequenceSync(String table, String identityColumn) {
        return null;
    }

    /** Caps a batch at 1000 rows regardless of column count (Oracle's bound-parameter limit is generous; this is a conservative fixed cap). */
    @Override
    public int maxBatchRows(int columnCount) {
        return 1000;
    }
}
