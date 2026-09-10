package vn.org.thn.app.base.persistence.dialect;

/** Per-database-engine SQL generation: paging, batch insert limits, identity/sequence handling. */
public interface SqlDialect {

    /** This dialect's short engine name (e.g. "postgresql", "oracle"), used for logging and a couple of engine-specific branches elsewhere in the ORM. */
    String getName();

    /** Max rows this engine can safely take in one flattened multi-row INSERT, given the row has {@code columnCount} columns (bounded by the engine's max bound-parameter count). */
    int maxBatchRows(int columnCount);

    /** Sequence-sync statement to run after a manual-id insert, or null if not applicable/needed. */
    String buildSequenceSync(String table, String identityColumn);

    /** Wraps {@code sql} with this engine's paging syntax (LIMIT/OFFSET, OFFSET/FETCH, TOP, ...). Either bound may be null (no limit / no offset). */
    String buildPagingSql(String sql, Integer limit, Integer offset);

    /** Builds an INSERT statement that also yields the generated identity value - see {@link #singleStatementReturning()} for whether it does so in one round trip. */
    String buildInsertReturning(String table, String columns, String params, String identityColumn);

    /** Wraps {@code insertSql} as needed to allow inserting an explicit value into an identity column (e.g. SQL Server's {@code SET IDENTITY_INSERT ... ON/OFF}). Returns {@code insertSql} unchanged where no such wrapping is needed. */
    String buildIdentityInsert(String table, String insertSql);

    /** Whether this engine supports the flattened multi-row {@code INSERT ... VALUES (...),(...),...} form used by {@code BatchExecutor}/{@code BatchInsertExecutor}. */
    boolean supportBatchInsert();

    /** Whether this engine supports auto-generated identity columns at all. */
    boolean supportIdentity();

    /**
     * True (the default, unchanged for every pre-existing dialect) when {@link #buildInsertReturning}
     * produces a single statement that can run as one query and hand back the new id directly - true
     * for Postgres/MySQL/SQL Server/SQLite, which each embed the id lookup in the same "INSERT ...;
     * SELECT ...;" string executed as one round trip. False means the engine has no plain-JDBC way to
     * do that through this module's generic "one raw SQL string -&gt; one query" executor (e.g. Oracle -
     * {@code RETURNING ... INTO} needs an OUT-bound {@code CallableStatement}, which doesn't fit that
     * shape) - the caller must instead run {@link #buildInsertReturning} as a plain INSERT, then
     * separately query {@link #buildIdentitySelect} for the new value.
     */
    default boolean singleStatementReturning() {
        return true;
    }

    /**
     * Only called when {@link #singleStatementReturning()} is false, right after the INSERT from
     * {@link #buildInsertReturning} has been run: a SELECT that returns the identity value that
     * insert just generated for {@code table}.
     */
    default String buildIdentitySelect(String table, String identityColumn) {
        throw new UnsupportedOperationException(getName() + " does not need buildIdentitySelect (singleStatementReturning() is true)");
    }
}
