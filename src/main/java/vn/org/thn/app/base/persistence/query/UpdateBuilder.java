package vn.org.thn.app.base.persistence.query;

import vn.org.thn.app.base.persistence.executor.QueryExecutor;
import vn.org.thn.app.base.persistence.lambda.SFunction;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent UPDATE builder. Obtained from {@code BaseRepository#update()}. A WHERE clause is
 * mandatory - {@link #execute()} throws rather than run an unconditional UPDATE, same as the
 * Kotlin original.
 */
public final class UpdateBuilder<T> extends BaseConditionBuilder<T, UpdateBuilder<T>> {

    private final Map<String, Object> setValues = new LinkedHashMap<>();

    public UpdateBuilder(Class<T> clazz, EntityInfo info, QueryExecutor queryExecutor) {
        super(clazz, info, queryExecutor);
    }

    /** Adds {@code field = value} to the SET clause. */
    public UpdateBuilder<T> set(String field, Object value) {
        setValues.put(column(field), value);
        return this;
    }

    /** {@link #set(String, Object)} with the field named via a method reference instead of a string. */
    public <R> UpdateBuilder<T> set(SFunction<T, R> field, R value) {
        return set(fieldName(field), value);
    }

    /**
     * Runs the UPDATE and returns the affected-row count. Throws {@link IllegalArgumentException}
     * if no {@code set(...)} value was supplied, and {@link IllegalStateException} if no WHERE
     * condition was added - an unconditional UPDATE is refused rather than silently touching every row.
     */
    public int execute() {
        if (setValues.isEmpty()) {
            throw new IllegalArgumentException("No update values");
        }
        if (whereClauses.isEmpty()) {
            throw new IllegalStateException("Update without WHERE is forbidden");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(info.getTableName()).append(" SET ");

        boolean first = true;
        for (Map.Entry<String, Object> e : setValues.entrySet()) {
            if (!first) sql.append(", ");
            sql.append(e.getKey()).append(" = #{set_").append(e.getKey()).append('}');
            first = false;
        }

        sql.append(" WHERE ").append(buildWhere());

        Map<String, Object> allParams = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : setValues.entrySet()) {
            allParams.put("set_" + e.getKey(), e.getValue());
        }
        allParams.putAll(params);

        return queryExecutor.execute(sql.toString(), allParams);
    }
}
