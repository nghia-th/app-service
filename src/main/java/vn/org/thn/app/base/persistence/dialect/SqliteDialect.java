package vn.org.thn.app.base.persistence.dialect;

/** {@link SqlDialect} for SQLite - the default engine when {@code base.database.type} is unset. */
public class SqliteDialect implements SqlDialect {

    @Override
    public String getName() {
        return "sqlite";
    }

    @Override
    public String buildPagingSql(String sql, Integer limit, Integer offset) {
        String limitPart = limit != null ? " LIMIT " + limit : "";
        String offsetPart = offset != null ? " OFFSET " + offset : "";
        return sql + limitPart + offsetPart;
    }

    /**
     * Plain INSERT with no identity lookup of its own - {@link #singleStatementReturning()} is
     * {@code false} below, so {@code InsertExecutor} always follows this up with a separate
     * {@link #buildIdentitySelect} query (SQLite's {@code last_insert_rowid()}, which reads back
     * the identity value from the INSERT run just before it on the same connection) rather than
     * expecting this method to return it in one round trip.
     */
    @Override
    public String buildInsertReturning(String table, String columns, String params, String identityColumn) {
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

    @Override
    public String buildSequenceSync(String table, String identityColumn) {
        return null;
    }

    @Override
    public int maxBatchRows(int columnCount) {
        return 1000;
    }

    @Override
    public boolean singleStatementReturning() {
        return false;
    }
    @Override
    public String buildIdentitySelect(String table, String identityColumn) {
        return "SELECT last_insert_rowid();";
    }
}
