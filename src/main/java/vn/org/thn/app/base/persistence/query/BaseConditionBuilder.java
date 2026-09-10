package vn.org.thn.app.base.persistence.query;


import vn.org.thn.app.base.persistence.executor.QueryExecutor;
import vn.org.thn.app.base.persistence.lambda.LambdaFieldResolver;
import vn.org.thn.app.base.persistence.lambda.SFunction;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared WHERE-clause building for {@link QueryBuilder}/{@link UpdateBuilder}/{@link DeleteBuilder}:
 * eq/ne/gt/ge/lt/le/like/startsWith/endsWith/in/notIn/between/isNull/isNotNull/orEq/raw, each with a
 * String-field-name overload and a method-reference ({@link SFunction}) overload. Ported from the
 * Kotlin original's {@code BaseConditionBuilder<T, SELF>} - the generic self-type is kept so every
 * condition method returns the concrete subtype for fluent chaining (QueryBuilder/UpdateBuilder/DeleteBuilder).
 * <p>
 * Note on {@link #COUNTER}: it is a single {@code static} counter shared by every builder instance
 * (of every entity type, on every thread), only ever used to mint short, builder-local parameter
 * names like {@code p42}. Sharing it across instances is safe because each builder keeps its own
 * private {@link #params} map, so two builders reusing the same counter value simply pick the same
 * placeholder name in two unrelated maps - there is no cross-builder collision, just a shared source
 * of "next number".
 */
public abstract class BaseConditionBuilder<T, SELF extends BaseConditionBuilder<T, SELF>> {

    private static final AtomicLong COUNTER = new AtomicLong();

    protected final Class<T> clazz;
    protected final EntityInfo info;
    protected final QueryExecutor queryExecutor;

    protected final List<QueryCondition> whereClauses = new ArrayList<>();
    protected final Map<String, Object> params = new LinkedHashMap<>();

    protected BaseConditionBuilder(Class<T> clazz, EntityInfo info, QueryExecutor queryExecutor) {
        this.clazz = clazz;
        this.info = info;
        this.queryExecutor = queryExecutor;
    }

    /** Resolves a method-reference field accessor (e.g. {@code User::getName}) to its declared field name. */
    protected <R> String fieldName(SFunction<T, R> property) {
        return LambdaFieldResolver.resolve(property);
    }

    /** Field name -> column name via the entity's reverse index; falls back to the field name itself
     *  (e.g. a raw/aliased expression that isn't a mapped entity field). */
    protected String column(String field) {
        String col = info.getFieldColumns().get(field);
        return col != null ? col : field;
    }

    /** Mints the next bind-parameter placeholder name ({@code p1}, {@code p2}, ...), wrapping back to 0 past one million to keep names short over a long-lived builder. */
    protected String nextParam() {
        return "p" + COUNTER.updateAndGet(v -> v > 1_000_000 ? 0 : v + 1);
    }

    /** Returns {@code this} cast to the concrete builder subtype, so every condition method below can return {@code SELF} for fluent chaining. */
    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

    /** Renders all accumulated WHERE fragments, joined by each fragment's own {@link QueryLogic} (AND/OR). */
    protected String buildWhere() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < whereClauses.size(); i++) {
            QueryCondition cond = whereClauses.get(i);
            if (i > 0) {
                sb.append(' ').append(cond.logic().name()).append(' ');
            }
            sb.append(cond.sql());
        }
        return sb.toString();
    }

    /** Adds a {@code field <> value} condition (AND-joined). No-op if {@code value} is null. */
    public SELF ne(String field, Object value) {
        if (value == null) return self();
        String key = nextParam();
        params.put(key, value);
        whereClauses.add(new QueryCondition(column(field) + " <> #{" + key + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #ne(String, Object)} with the field named via a method reference instead of a string. */
    public <R> SELF ne(SFunction<T, R> field, R value) {
        return ne(fieldName(field), value);
    }

    /** Adds a {@code field > value} condition (AND-joined). No-op if {@code value} is null. */
    public SELF gt(String field, Object value) {
        if (value == null) return self();
        String key = nextParam();
        params.put(key, value);
        whereClauses.add(new QueryCondition(column(field) + " > #{" + key + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #gt(String, Object)} with the field named via a method reference instead of a string. */
    public <R> SELF gt(SFunction<T, R> field, R value) {
        return gt(fieldName(field), value);
    }

    /** Adds a {@code field >= value} condition (AND-joined). No-op if {@code value} is null. */
    public SELF ge(String field, Object value) {
        if (value == null) return self();
        String key = nextParam();
        params.put(key, value);
        whereClauses.add(new QueryCondition(column(field) + " >= #{" + key + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #ge(String, Object)} with the field named via a method reference instead of a string. */
    public <R> SELF ge(SFunction<T, R> field, R value) {
        return ge(fieldName(field), value);
    }

    /** Adds a {@code field < value} condition (AND-joined). No-op if {@code value} is null. */
    public SELF lt(String field, Object value) {
        if (value == null) return self();
        String key = nextParam();
        params.put(key, value);
        whereClauses.add(new QueryCondition(column(field) + " < #{" + key + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #lt(String, Object)} with the field named via a method reference instead of a string. */
    public <R> SELF lt(SFunction<T, R> field, R value) {
        return lt(fieldName(field), value);
    }

    /** Adds a {@code field <= value} condition (AND-joined). No-op if {@code value} is null. */
    public SELF le(String field, Object value) {
        if (value == null) return self();
        String key = nextParam();
        params.put(key, value);
        whereClauses.add(new QueryCondition(column(field) + " <= #{" + key + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #le(String, Object)} with the field named via a method reference instead of a string. */
    public <R> SELF le(SFunction<T, R> field, R value) {
        return le(fieldName(field), value);
    }

    /** Adds a {@code field = value} condition (AND-joined). No-op if {@code value} is null. */
    public SELF eq(String field, Object value) {
        if (value == null) return self();
        String key = nextParam();
        params.put(key, value);
        whereClauses.add(new QueryCondition(column(field) + " = #{" + key + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #eq(String, Object)} with the field named via a method reference instead of a string. */
    public <R> SELF eq(SFunction<T, R> field, R value) {
        return eq(fieldName(field), value);
    }

    /** Adds a {@code field LIKE '%value%'} condition (AND-joined). No-op if {@code value} is null/blank. */
    public SELF like(String field, String value) {
        if (value == null || value.isBlank()) return self();
        String key = nextParam();
        params.put(key, "%" + value + "%");
        whereClauses.add(new QueryCondition(column(field) + " LIKE #{" + key + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #like(String, String)} with the field named via a method reference instead of a string. */
    public SELF like(SFunction<T, ?> field, String value) {
        return like(fieldName(field), value);
    }

    /** Adds a {@code field LIKE 'value%'} (prefix match) condition (AND-joined). No-op if {@code value} is null/blank. */
    public SELF startsWith(String field, String value) {
        if (value == null || value.isBlank()) return self();
        String key = nextParam();
        params.put(key, value + "%");
        whereClauses.add(new QueryCondition(column(field) + " LIKE #{" + key + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #startsWith(String, String)} with the field named via a method reference instead of a string. */
    public SELF startsWith(SFunction<T, ?> field, String value) {
        return startsWith(fieldName(field), value);
    }

    /** Adds a {@code field LIKE '%value'} (suffix match) condition (AND-joined). No-op if {@code value} is null/blank. */
    public SELF endsWith(String field, String value) {
        if (value == null || value.isBlank()) return self();
        String key = nextParam();
        params.put(key, "%" + value);
        whereClauses.add(new QueryCondition(column(field) + " LIKE #{" + key + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #endsWith(String, String)} with the field named via a method reference instead of a string. */
    public SELF endsWith(SFunction<T, ?> field, String value) {
        return endsWith(fieldName(field), value);
    }

    /** Adds a {@code field IN (...)} condition (AND-joined), one bind parameter per value. No-op if {@code values} is null/empty. */
    public SELF in(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) return self();
        List<String> placeholders = new ArrayList<>();
        for (Object v : values) {
            String key = nextParam();
            params.put(key, v);
            placeholders.add("#{" + key + "}");
        }
        whereClauses.add(new QueryCondition(column(field) + " IN (" + String.join(",", placeholders) + ")", QueryLogic.AND));
        return self();
    }

    /** {@link #in(String, Collection)} with the field named via a method reference instead of a string. */
    public <R> SELF in(SFunction<T, R> field, Collection<R> values) {
        return in(fieldName(field), values);
    }

    /** Adds a {@code field NOT IN (...)} condition (AND-joined), one bind parameter per value. No-op if {@code values} is null/empty. */
    public SELF notIn(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) return self();
        List<String> placeholders = new ArrayList<>();
        for (Object v : values) {
            String key = nextParam();
            params.put(key, v);
            placeholders.add("#{" + key + "}");
        }
        whereClauses.add(new QueryCondition(column(field) + " NOT IN (" + String.join(",", placeholders) + ")", QueryLogic.AND));
        return self();
    }

    /** {@link #notIn(String, Collection)} with the field named via a method reference instead of a string. */
    public <R> SELF notIn(SFunction<T, R> field, Collection<R> values) {
        return notIn(fieldName(field), values);
    }

    /** Adds a {@code field BETWEEN start AND end} condition (AND-joined). No-op if either bound is null. */
    public SELF between(String field, Object start, Object end) {
        if (start == null || end == null) return self();
        String p1 = nextParam();
        params.put(p1, start);
        String p2 = nextParam();
        params.put(p2, end);
        whereClauses.add(new QueryCondition(column(field) + " BETWEEN #{" + p1 + "} AND #{" + p2 + "}", QueryLogic.AND));
        return self();
    }

    /** {@link #between(String, Object, Object)} with the field named via a method reference instead of a string. */
    public <R> SELF between(SFunction<T, R> field, R start, R end) {
        return between(fieldName(field), start, end);
    }

    /** Adds a {@code field IS NULL} condition (AND-joined). */
    public SELF isNull(String field) {
        whereClauses.add(new QueryCondition(column(field) + " IS NULL", QueryLogic.AND));
        return self();
    }

    /** {@link #isNull(String)} with the field named via a method reference instead of a string. */
    public SELF isNull(SFunction<T, ?> field) {
        return isNull(fieldName(field));
    }

    /** Adds a {@code field IS NOT NULL} condition (AND-joined). */
    public SELF isNotNull(String field) {
        whereClauses.add(new QueryCondition(column(field) + " IS NOT NULL", QueryLogic.AND));
        return self();
    }

    /** {@link #isNotNull(String)} with the field named via a method reference instead of a string. */
    public SELF isNotNull(SFunction<T, ?> field) {
        return isNotNull(fieldName(field));
    }

    /** Adds a {@code field = value} condition, OR-joined with whatever conditions came before it. No-op if {@code value} is null. */
    public SELF orEq(String field, Object value) {
        if (value == null) return self();
        String key = nextParam();
        params.put(key, value);
        whereClauses.add(new QueryCondition(column(field) + " = #{" + key + "}", QueryLogic.OR));
        return self();
    }

    /** {@link #orEq(String, Object)} with the field named via a method reference instead of a string. */
    public <R> SELF orEq(SFunction<T, R> field, R value) {
        return orEq(fieldName(field), value);
    }

    /** Adds a raw, already-rendered SQL fragment as a WHERE condition (AND-joined), with no bind parameters. */
    public SELF raw(String sql) {
        return raw(sql, Map.of());
    }

    /** Adds a raw, already-rendered SQL fragment as a WHERE condition (AND-joined), together with the bind parameters it references. Throws if {@code sql} is null/blank. */
    public SELF raw(String sql, Map<String, Object> values) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql must not be blank");
        }
        whereClauses.add(new QueryCondition(sql, QueryLogic.AND));
        params.putAll(values);
        return self();
    }
}
