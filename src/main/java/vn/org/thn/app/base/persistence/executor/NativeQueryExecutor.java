package vn.org.thn.app.base.persistence.executor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Thin convenience layer over {@link QueryExecutor} for services that need to run raw SQL
 * directly (reporting queries, migration helpers, anything the fluent DSL doesn't cover) while
 * still getting the same paging/logging/row-mapping behavior as the rest of the ORM.
 * <p>
 * The Kotlin original's {@code listJson}/{@code oneJson} returned a Jackson {@code ObjectNode};
 * this module returns plain {@code Map<String, Object>} instead (same as {@link #listMap}/
 * {@link #oneMap}) - a plain map serializes to identical JSON through Spring's message converter,
 * without pulling Jackson's tree API into the ORM's public surface. That made the two pairs of
 * methods byte-for-byte identical, so the separate {@code listJson}/{@code oneJson} aliases were
 * dropped (confirmed zero callers anywhere in this repo) instead of being kept as a second name
 * for the exact same behavior.
 */
@Component
public class NativeQueryExecutor {

    private final QueryExecutor queryExecutor;

    public NativeQueryExecutor(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    /** Raw SQL -> list of {@code clazz} instances, paged. */
    public <T> List<T> list(String sql, Class<T> clazz, Map<String, Object> params, Integer limit, Integer offset) {
        return queryExecutor.selectList(sql, clazz, params, limit, offset);
    }

    /** Raw SQL -> list of {@code clazz} instances, unpaged. */
    public <T> List<T> list(String sql, Class<T> clazz, Map<String, Object> params) {
        return list(sql, clazz, params, null, null);
    }

    /** Raw SQL -> single {@code clazz} instance from the first row, or null if no row matched. */
    public <T> T one(String sql, Class<T> clazz, Map<String, Object> params) {
        return queryExecutor.selectOne(sql, clazz, params);
    }

    /** Raw SQL -> list of rows, each as a column-name -> value map, paged. */
    public List<Map<String, Object>> listMap(String sql, Map<String, Object> params, Integer limit, Integer offset) {
        return queryExecutor.selectList(sql, params, limit, offset);
    }

    /** Raw SQL -> list of rows, each as a column-name -> value map, unpaged. */
    public List<Map<String, Object>> listMap(String sql, Map<String, Object> params) {
        return listMap(sql, params, null, null);
    }

    /** Raw SQL -> single row as a column-name -> value map, or null if no row matched. */
    public Map<String, Object> oneMap(String sql, Map<String, Object> params) {
        return queryExecutor.selectOne(sql, params);
    }

    /** Raw SQL -> first column of the first row (e.g. a {@code COUNT(*)} or a single aggregate value). */
    public Object value(String sql, Map<String, Object> params) {
        return queryExecutor.selectValue(sql, params);
    }
}
