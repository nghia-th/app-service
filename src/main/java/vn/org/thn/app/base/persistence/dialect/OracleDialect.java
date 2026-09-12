package vn.org.thn.app.base.persistence.dialect;

/**
 * Targets Oracle 12c and newer (ANSI OFFSET/FETCH paging, IDENTITY columns). Older Oracle
 * (ROWNUM-based paging, sequence + BEFORE INSERT trigger for ids) is not supported.
 * <p>
 * Unlike Postgres/MySQL/SQL Server/SQLite, plain JDBC has no way to combine "INSERT" and "read
 * back the new id" into one query for Oracle via this module's generic "one raw SQL string -&gt;
 * one query" executor - see {@link #singleStatementReturning()}: {@code false} here means
 * {@code InsertExecutor} does not use {@link #buildInsertReturning} to fetch the id directly.
 * Instead, Oracle's {@code RETURNING ... INTO} clause is used the way it actually works on the
 * JDBC driver - bound to an OUT parameter on a {@code CallableStatement}, via
 * {@link #buildInsertReturningCallable} - which still completes the INSERT and the id lookup in a
 * single round trip and is safe under concurrent inserts into the same table, unlike a follow-up
 * {@code SELECT MAX(id)}. {@link #buildIdentitySelect} (the {@code SELECT MAX(id)} approach) is
 * kept only as a documented last-resort fallback and is not used in practice, since
 * {@link #buildInsertReturningCallable} always returns a usable statement whenever this entity
 * has an identity column.
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

    /**
     * Single-round-trip, concurrency-safe identity retrieval for Oracle: an anonymous PL/SQL block
     * that inserts the row and binds the newly generated {@code identityColumn} value to
     * {@link #RETURNING_ID_PARAM} via {@code RETURNING ... INTO}, executed as a CallableStatement
     * (see mapper/DynamicSQL.xml's {@code executeCallable} statement) - see the class doc.
     */
    @Override
    public String buildInsertReturningCallable(String table, String columns, String params, String identityColumn) {
        if (identityColumn == null) {
            return null;
        }
        return "BEGIN INSERT INTO " + table + " (" + columns + ") VALUES (" + params + ") RETURNING "
                + identityColumn + " INTO #{" + RETURNING_ID_PARAM + ", mode=OUT, jdbcType=NUMERIC}; END;";
    }

    /**
     * Last-resort fallback, not used in practice - see the class doc and
     * {@link #buildInsertReturningCallable}. Kept documented in case a future caller needs identity
     * retrieval with no identity column info available to build the callable form; a plain
     * {@code SELECT MAX(id)} is NOT safe under concurrent inserts into the same table.
     */
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
