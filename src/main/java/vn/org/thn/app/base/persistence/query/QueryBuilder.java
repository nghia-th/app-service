package vn.org.thn.app.base.persistence.query;


import vn.org.thn.app.base.core.dto.page.PageResponse;
import vn.org.thn.app.base.persistence.executor.QueryExecutor;
import vn.org.thn.app.base.persistence.lambda.SFunction;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Fluent, type-safe SELECT builder - the main entry point of the query DSL. Obtained from
 * {@code BaseRepository#query()}. Ported from the Kotlin original's {@code QueryBuilder<T>};
 * {@code and { }}/{@code or { }} lambda-with-receiver blocks become {@code Consumer<QueryBuilder<T>>}.
 */
public final class QueryBuilder<T> extends BaseConditionBuilder<T, QueryBuilder<T>> {

    final List<String> selectColumns = new ArrayList<>();
    final List<QueryOrder> orderByClauses = new ArrayList<>();

    private Integer limitValue;
    private Integer offsetValue;
    private Integer pageNumber;
    private Integer pageSize;
    private String alias;

    public QueryBuilder(Class<T> clazz, EntityInfo info, QueryExecutor queryExecutor) {
        super(clazz, info, queryExecutor);
    }

    /** Sets a table alias to prefix the {@code FROM} clause with (e.g. for use in raw/joined SQL fragments). */
    public QueryBuilder<T> alias(String name) {
        this.alias = name;
        return this;
    }

    /** Restricts the SELECT list to the given entity field names (resolved to columns), instead of {@code SELECT *}. */
    public QueryBuilder<T> select(String... fields) {
        for (String f : fields) {
            selectColumns.add(column(f));
        }
        return this;
    }

    /** {@link #select(String...)} with fields named via method references instead of strings. */
    @SafeVarargs
    public final QueryBuilder<T> select(SFunction<T, ?>... fields) {
        for (SFunction<T, ?> f : fields) {
            selectColumns.add(column(fieldName(f)));
        }
        return this;
    }

    /** Adds a raw, already-rendered SQL expression to the SELECT list (e.g. a computed column or aggregate). */
    public QueryBuilder<T> selectRaw(String sql) {
        selectColumns.add(sql);
        return this;
    }

    /** Caps the number of rows returned. */
    public QueryBuilder<T> limit(int value) {
        this.limitValue = value;
        return this;
    }

    /** Skips this many rows before returning results. */
    public QueryBuilder<T> offset(int value) {
        this.offsetValue = value;
        return this;
    }

    /** Adds an {@code ORDER BY field ASC} clause. */
    public QueryBuilder<T> orderByAsc(String field) {
        orderByClauses.add(new QueryOrder(column(field), QueryDirection.ASC));
        return this;
    }

    /** {@link #orderByAsc(String)} with the field named via a method reference instead of a string. */
    public QueryBuilder<T> orderByAsc(SFunction<T, ?> field) {
        return orderByAsc(fieldName(field));
    }

    /** Adds an {@code ORDER BY field DESC} clause. */
    public QueryBuilder<T> orderByDesc(String field) {
        orderByClauses.add(new QueryOrder(column(field), QueryDirection.DESC));
        return this;
    }

    /** {@link #orderByDesc(String)} with the field named via a method reference instead of a string. */
    public QueryBuilder<T> orderByDesc(SFunction<T, ?> field) {
        return orderByDesc(fieldName(field));
    }

    /** Renders the full {@code SELECT ... FROM ... [WHERE ...] [ORDER BY ...]} statement from the builder's current state. */
    String buildSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        sb.append(selectColumns.isEmpty() ? "*" : String.join(", ", selectColumns));
        sb.append(" FROM ").append(info.getTableName());

        if (alias != null) {
            sb.append(' ').append(alias);
        }

        if (!whereClauses.isEmpty()) {
            sb.append(" WHERE ").append(buildWhere());
        }

        if (!orderByClauses.isEmpty()) {
            sb.append(" ORDER BY ");
            List<String> parts = new ArrayList<>();
            for (QueryOrder o : orderByClauses) {
                parts.add(o.field() + " " + o.direction().name());
            }
            sb.append(String.join(", ", parts));
        }

        return sb.toString();
    }

    /** Runs the query and returns every matching row, mapped to {@code T}. */
    public List<T> list() {
        return queryExecutor.selectList(buildSql(), clazz, params, limitValue, offsetValue);
    }

    /** Runs the query forcing a limit of 1 and returns the first matching row, or null if none matched. Restores whatever limit was set before the call. */
    public T one() {
        Integer oldLimit = limitValue;
        limit(1);
        List<T> result = list();
        limitValue = oldLimit;
        return result.isEmpty() ? null : result.get(0);
    }

    /** Resets every accumulated clause (select/where/params/order/paging/alias) so the builder can be reused from scratch. */
    public QueryBuilder<T> clear() {
        selectColumns.clear();
        whereClauses.clear();
        params.clear();
        orderByClauses.clear();
        limitValue = null;
        offsetValue = null;
        pageNumber = null;
        pageSize = null;
        alias = null;
        return this;
    }

    /** Runs {@code SELECT COUNT(*)} over the builder's current WHERE clause (select list and paging are ignored). */
    public long count() {
        String sql = queryExecutor.buildCountSql(buildSql());
        Object value = queryExecutor.selectValue(sql, params);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /** Runs {@code SELECT COUNT(field)} for the given field (excludes nulls, per standard SQL COUNT semantics). */
    public long count(SFunction<T, ?> field) {
        Number result = aggregate("COUNT(" + column(fieldName(field)) + ")");
        return result == null ? 0L : result.longValue();
    }

    /** Renders the current SELECT statement without executing it - useful for logging/debugging. */
    public String toSql() {
        return buildSql();
    }

    /** Whether the current WHERE clause matches at least one row. */
    public boolean exists() {
        return count() > 0;
    }

    /** Alias for {@link #one()}. */
    public T first() {
        return one();
    }

    /** Returns the last row per the current {@code ORDER BY} (implemented by temporarily reversing every order-by direction and taking the first row); requires at least one {@code orderBy} clause to be set. Restores the original order-by/limit state before returning, even on failure. */
    public T last() {
        if (orderByClauses.isEmpty()) {
            throw new IllegalStateException("last() requires orderBy");
        }

        Integer oldLimit = limitValue;
        List<QueryOrder> oldOrders = new ArrayList<>(orderByClauses);

        try {
            limitValue = 1;
            orderByClauses.clear();
            for (QueryOrder o : oldOrders) {
                orderByClauses.add(new QueryOrder(
                        o.field(),
                        o.direction() == QueryDirection.ASC ? QueryDirection.DESC : QueryDirection.ASC));
            }
            List<T> result = list();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            orderByClauses.clear();
            orderByClauses.addAll(oldOrders);
            limitValue = oldLimit;
        }
    }

    /** 1-based page number, matching the Kotlin original's DSL (unlike {@link vn.org.thn.service.base.dto.page.PageRequest}, which is 0-based). */
    public QueryBuilder<T> page(int page, int size) {
        if (page <= 0) throw new IllegalArgumentException("page must be greater than 0");
        if (size <= 0) throw new IllegalArgumentException("size must be greater than 0");

        this.pageNumber = page;
        this.pageSize = size;
        this.offsetValue = (page - 1) * size;
        this.limitValue = size;
        return this;
    }

    /** Runs both the count query and the page's data query and assembles a {@link PageResponse}. Defaults to page 1 / size 20 if {@link #page(int, int)} was never called. */
    public PageResponse<T> pageResult() {
        int page = pageNumber != null ? pageNumber : 1;
        int size = pageSize != null ? pageSize : 20;
        long total = count();
        List<T> data = list();
        return PageResponse.of(data, page, size, total);
    }

    /** Same as {@link #toSql()} - the rendered SELECT statement. */
    @Override
    public String toString() {
        return buildSql();
    }

    /**
     * Builds a nested sub-builder, lets {@code block} add conditions to it, then folds the whole
     * group into this builder as one parenthesized, AND-joined fragment (e.g. {@code AND (a = ? OR b = ?)}).
     * No-op if the block added no conditions.
     */
    public QueryBuilder<T> and(Consumer<QueryBuilder<T>> block) {
        QueryBuilder<T> nested = new QueryBuilder<>(clazz, info, queryExecutor);
        block.accept(nested);
        if (!nested.whereClauses.isEmpty()) {
            whereClauses.add(new QueryCondition("(" + nested.buildWhere() + ")", QueryLogic.AND));
            params.putAll(nested.params);
        }
        return this;
    }

    /**
     * Builds a nested sub-builder, lets {@code block} add conditions to it, then folds the whole
     * group into this builder as one parenthesized, OR-joined fragment (e.g. {@code OR (a = ? AND b = ?)}).
     * No-op if the block added no conditions.
     */
    public QueryBuilder<T> or(Consumer<QueryBuilder<T>> block) {
        QueryBuilder<T> nested = new QueryBuilder<>(clazz, info, queryExecutor);
        block.accept(nested);
        if (!nested.whereClauses.isEmpty()) {
            whereClauses.add(new QueryCondition("(" + nested.buildWhere() + ")", QueryLogic.OR));
            params.putAll(nested.params);
        }
        return this;
    }

    /** Runs the query selecting only {@code field} and returns that single column's values for every matching row. Temporarily overrides, then restores, the select list. */
    @SuppressWarnings("unchecked")
    public <R> List<R> listValue(SFunction<T, R> field) {
        List<String> oldSelect = new ArrayList<>(selectColumns);
        try {
            selectColumns.clear();
            selectColumns.add(column(fieldName(field)));
            List<Object> result = queryExecutor.selectColumn(buildSql(), params);
            if (result == null) {
                return List.of();
            }
            List<R> out = new ArrayList<>(result.size());
            for (Object o : result) {
                out.add((R) o);
            }
            return out;
        } finally {
            selectColumns.clear();
            selectColumns.addAll(oldSelect);
        }
    }

    /** First value from {@link #listValue(SFunction)}, or null if no row matched. */
    public <R> R oneValue(SFunction<T, R> field) {
        List<R> values = listValue(field);
        return values.isEmpty() ? null : values.get(0);
    }

    /** Runs the query selecting only the given raw aggregate expression (e.g. {@code MAX(col)}) and returns its scalar value. Temporarily overrides, then restores, the select list. */
    @SuppressWarnings("unchecked")
    private <R> R aggregate(String sqlPart) {
        List<String> oldSelect = new ArrayList<>(selectColumns);
        try {
            selectColumns.clear();
            selectColumns.add(sqlPart);
            return (R) queryExecutor.selectValue(buildSql(), params);
        } finally {
            selectColumns.clear();
            selectColumns.addAll(oldSelect);
        }
    }

    /** Runs {@code SELECT MAX(field)} over the current WHERE clause. */
    public <R> R max(SFunction<T, R> field) {
        return aggregate("MAX(" + column(fieldName(field)) + ")");
    }

    /** Runs {@code SELECT MIN(field)} over the current WHERE clause. */
    public <R> R min(SFunction<T, R> field) {
        return aggregate("MIN(" + column(fieldName(field)) + ")");
    }

    /** Runs {@code SELECT SUM(field)} over the current WHERE clause. */
    public <R> R sum(SFunction<T, R> field) {
        return aggregate("SUM(" + column(fieldName(field)) + ")");
    }

    /** Runs {@code SELECT AVG(field)} over the current WHERE clause, or null if no row matched. */
    public Double avg(SFunction<T, ?> field) {
        Number result = aggregate("AVG(" + column(fieldName(field)) + ")");
        return result == null ? null : result.doubleValue();
    }

    /** {@link #max(SFunction)}, defaulting to 0 instead of null when no row matched or the max isn't numeric. */
    public long maxOrZero(SFunction<T, ?> field) {
        Object result = max(field);
        return result instanceof Number number ? number.longValue() : 0L;
    }

    /** {@link #sum(SFunction)}, defaulting to 0.0 instead of null when no row matched or the sum isn't numeric. */
    public double sumOrZero(SFunction<T, ?> field) {
        Object result = sum(field);
        return result instanceof Number number ? number.doubleValue() : 0.0;
    }

    /** {@link #avg(SFunction)}, defaulting to 0.0 instead of null when no row matched. */
    public double avgOrZero(SFunction<T, ?> field) {
        Double result = avg(field);
        return result == null ? 0.0 : result;
    }

    /** Whether the current WHERE clause matches no rows - the inverse of {@link #exists()}. */
    public boolean empty() {
        return !exists();
    }

    /** {@link #one()}, throwing {@link NoSuchElementException} instead of returning null when no row matched. */
    public T oneOrThrow() {
        T result = one();
        if (result == null) {
            throw new NoSuchElementException(clazz.getSimpleName() + " not found");
        }
        return result;
    }

    /** {@link #first()}, throwing {@link NoSuchElementException} instead of returning null when no row matched. */
    public T firstOrThrow() {
        T result = first();
        if (result == null) {
            throw new NoSuchElementException(clazz.getSimpleName() + " not found");
        }
        return result;
    }
}
