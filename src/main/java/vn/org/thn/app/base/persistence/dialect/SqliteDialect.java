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

    /** SQLite's {@code last_insert_rowid()} reads back the identity value from the INSERT run just before it on the same connection. */
    @Override
    public String buildInsertReturning(String table, String columns, String params, String identityColumn) {
        return "INSERT INTO " + table + " (" + columns + ")\n"
                + "VALUES (" + params + ");\n\n";
//                + "SELECT last_insert_rowid();";
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
