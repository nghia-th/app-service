package vn.org.thn.app.base.persistence.executor;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.persistence.dialect.SqlDialect;
import vn.org.thn.app.base.persistence.logging.SqlLogResult;
import vn.org.thn.app.base.persistence.logging.SqlLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Runs the read side of the DSL: builds the wrapper param map, applies the dialect's paging SQL,
 * logs (via {@link SqlLogger}), executes through the generic {@code DynamicSQL} mapper, and maps
 * rows to the caller's target type via {@link RowMapper}.
 */
@Component
public class QueryExecutor {

    private static final Pattern ORDER_BY = Pattern.compile("order\\s+by[\\s\\S]*$", Pattern.CASE_INSENSITIVE);

    private final SqlSessionTemplate session;
    private final SqlLogger sqlLogger;
    private final SqlDialect dialect;

    public QueryExecutor(SqlSessionTemplate session, SqlLogger sqlLogger, SqlDialect dialect) {
        this.session = session;
        this.sqlLogger = sqlLogger;
        this.dialect = dialect;
    }

    /** Builds the {sql, ...params} map the generic {@code DynamicSQL} mapper statements expect. */
    private Map<String, Object> wrapper(String sql, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("sql", sql);
        if (params != null) {
            result.putAll(params);
        }
        return result;
    }

    /** Wraps {@code sql} in a {@code SELECT COUNT(*) FROM (...)}, stripping any trailing {@code ORDER BY} first (irrelevant to a count and rejected by some engines inside a subquery). */
    public String buildCountSql(String sql) {
        String cleanSql = ORDER_BY.matcher(sql).replaceAll("");
        return "SELECT COUNT(*) AS total FROM (" + cleanSql + ") tmp";
    }

    /** Runs a write statement (INSERT/UPDATE/DELETE), logging it and its timing, and returns the affected-row count. */
    public int execute(String sql, Map<String, Object> params) {
        Map<String, Object> p = params != null ? params : Map.of();
        sqlLogger.log(sql, p);
        long start = System.currentTimeMillis();

        int affected = session.update("DynamicSQL.execute", wrapper(sql, params));

        long end = System.currentTimeMillis();
        sqlLogger.finish(new SqlLogResult(affected, end - start));
        return affected;
    }

    /** Runs a SELECT, paged per the active dialect, mapping each row onto {@code clazz} via {@link RowMapper}. */
    @SuppressWarnings("unchecked")
    public <T> List<T> selectList(String sql, Class<T> clazz, Map<String, Object> params, Integer limit, Integer offset) {
        List<Map<String, Object>> rows = selectList(sql, params, limit, offset);
        List<T> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(RowMapper.map(row, clazz));
        }
        return result;
    }

    /** Runs a SELECT, paged per the active dialect, returning each row as a column-name -> value map. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> selectList(String sql, Map<String, Object> params, Integer limit, Integer offset) {
        String finalSql = dialect.buildPagingSql(sql, limit, offset);
        Map<String, Object> p = params != null ? params : Map.of();
        sqlLogger.log(finalSql, p);
        long start = System.currentTimeMillis();

        List<Object> rawList = session.selectList("DynamicSQL.selectList", wrapper(finalSql, params));
        if (rawList == null) {
            rawList = List.of();
        }

        long end = System.currentTimeMillis();
        sqlLogger.finish(new SqlLogResult(rawList.size(), end - start));

        List<Map<String, Object>> rows = new ArrayList<>(rawList.size());
        for (Object o : rawList) {
            if (o instanceof Map<?, ?> rawRow) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : rawRow.entrySet()) {
                    row.put(String.valueOf(e.getKey()), e.getValue());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    /** First row of {@code sql} (forces {@code LIMIT 1 OFFSET 0}), mapped onto {@code clazz}, or null if no row matched. */
    public <T> T selectOne(String sql, Class<T> clazz, Map<String, Object> params) {
        List<T> result = selectList(sql, clazz, params, 1, 0);
        return result.isEmpty() ? null : result.get(0);
    }

    /** First row of {@code sql} as a column-name -> value map, or null if no row matched. */
    public Map<String, Object> selectOne(String sql, Map<String, Object> params) {
        List<Map<String, Object>> result = selectList(sql, params, 1, 0);
        return result.isEmpty() ? null : result.get(0);
    }

    /** First column of the first row (e.g. a {@code COUNT(*)}, a generated id, a single aggregate), or null if no row matched. */
    public Object selectValue(String sql, Map<String, Object> params) {
        List<Object> result = selectColumn(sql, params);
        return result.isEmpty() ? null : result.get(0);
    }

    /** First column of every row - unpaged, no {@link RowMapper} involved (values are returned as-is). */
    public List<Object> selectColumn(String sql, Map<String, Object> params) {
        Map<String, Object> p = params != null ? params : Map.of();
        sqlLogger.log(sql, p);
        long start = System.currentTimeMillis();

        List<Object> rawList = session.selectList("DynamicSQL.selectList", wrapper(sql, params));
        if (rawList == null) {
            rawList = List.of();
        }

        long end = System.currentTimeMillis();
        sqlLogger.finish(new SqlLogResult(rawList.size(), end - start));

        List<Object> result = new ArrayList<>(rawList.size());
        for (Object o : rawList) {
            if (o instanceof Map<?, ?> row) {
                result.add(row.values().stream().findFirst().orElse(null));
            } else {
                result.add(o);
            }
        }
        return result;
    }

    /** The active {@link SqlDialect}'s name (e.g. "postgresql", "oracle") - mainly for logging/diagnostics. */
    public String dialectName() {
        return dialect.getName();
    }
}
