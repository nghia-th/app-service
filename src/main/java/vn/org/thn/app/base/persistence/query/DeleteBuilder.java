package vn.org.thn.app.base.persistence.query;


import vn.org.thn.app.base.persistence.executor.QueryExecutor;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;

/**
 * Fluent DELETE builder. Obtained from {@code BaseRepository#delete()}. A WHERE clause is
 * mandatory - {@link #execute()} throws rather than run an unconditional DELETE.
 */
public final class DeleteBuilder<T> extends BaseConditionBuilder<T, DeleteBuilder<T>> {

    public DeleteBuilder(Class<T> clazz, EntityInfo info, QueryExecutor queryExecutor) {
        super(clazz, info, queryExecutor);
    }

    /**
     * Runs the DELETE and returns the affected-row count. Throws {@link IllegalStateException} if
     * no WHERE condition was added - an unconditional DELETE is refused rather than silently
     * wiping the table.
     */
    public int execute() {
        if (whereClauses.isEmpty()) {
            throw new IllegalStateException("Delete without WHERE is forbidden");
        }

        String sql = "DELETE FROM " + info.getTableName() + " WHERE " + buildWhere();
        return queryExecutor.execute(sql, params);
    }
}
