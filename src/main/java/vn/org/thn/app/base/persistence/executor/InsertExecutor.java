package vn.org.thn.app.base.persistence.executor;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.app.base.core.entity.BaseEntity;
import vn.org.thn.app.base.persistence.dialect.SqlDialect;
import vn.org.thn.app.base.persistence.metadata.EntityCache;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * INSERT / upsert logic, most notably {@link #save(Object)} - a single entry point that decides,
 * per call, whether to INSERT (letting the DB generate an identity value), UPDATE (row already
 * exists), or INSERT with a caller-supplied key, based on which primary-key columns are populated
 * and whether the entity has an identity column at all. Ported case-by-case from the Kotlin
 * original, including its 5-branch decision tree.
 */
@Component
public class InsertExecutor extends BaseExecutor {

    private final QueryExecutor queryExecutor;
    private final SqlDialect dialect;

    public InsertExecutor(SqlSessionTemplate session, QueryExecutor queryExecutor, SqlDialect dialect) {
        super(session);
        this.queryExecutor = queryExecutor;
        this.dialect = dialect;
    }

    /** Whether a primary-key value counts as "not supplied" - null, a blank string, or the numeric zero a not-yet-assigned identity field defaults to. */
    boolean isEmptyValue(Object v) {
        if (v == null) return true;
        if (v instanceof Number number) return number.longValue() == 0L;
        if (v instanceof String s) return s.isBlank();
        return false;
    }

    /** Writes a freshly generated identity value back onto the entity's id field (direct field access first, {@link FieldUtils} as fallback). */
    <T> void writeId(T entity, EntityInfo info, Object newId) {
        String idCol = info.getIdentityColumn();
        if (idCol == null) return;
        Field field = info.getFieldMap().get(idCol);
        // The DB returns whatever numeric type its driver picks (SQLite -> Integer, MySQL -> Long/BigInteger).
        // Coerce it to the entity's declared id type so the reflective set doesn't fail on a type mismatch.
        Class<?> targetType = field != null ? field.getType() : null;
        Object coercedId = coerceToFieldType(newId, targetType);
        try {
            if (field != null) {
                field.set(entity, coercedId);
                return;
            }
        } catch (Exception ignored) {
            // fall through to FieldUtils
        }
        try {
            FieldUtils.writeField(entity, info.getColumns().get(idCol), coercedId, true);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot write generated id to " + entity.getClass().getName(), e);
        }
    }

    /** Convert a DB-returned numeric id to the entity's declared id field type. Returns the value
     *  unchanged when no conversion is needed or the type isn't a supported number. */
    private static Object coerceToFieldType(Object value, Class<?> targetType) {
        if (value == null || targetType == null || targetType.isInstance(value)) {
            return value;
        }
        if (value instanceof Number n) {
            if (targetType == Long.class || targetType == long.class) return n.longValue();
            if (targetType == Integer.class || targetType == int.class) return n.intValue();
            if (targetType == Short.class || targetType == short.class) return n.shortValue();
            if (targetType == java.math.BigInteger.class) return java.math.BigInteger.valueOf(n.longValue());
            if (targetType == java.math.BigDecimal.class) return java.math.BigDecimal.valueOf(n.longValue());
        }
        return value;
    }

    /** Plain INSERT with no identity handling - used for composite-key entities with no auto-generated column. */
    void insert(Set<String> columns, EntityInfo info, Map<String, Object> paramMap) {
        String cols = String.join(",", columns);
        String params = String.join(",", columns.stream().map(c -> "#{" + c + "}").toList());
        runSql("INSERT INTO " + info.getTableName() + " (" + cols + ") VALUES (" + params + ")", paramMap);
    }

    /** INSERT where the caller supplies the identity column's value explicitly (dialect wraps it as needed, e.g. SQL Server's {@code IDENTITY_INSERT ON/OFF}); on Postgres, also fast-forwards the sequence so it doesn't collide with this manually-assigned id on a later auto insert. */
    void insertWithIdentity(Set<String> columns, EntityInfo info, Map<String, Object> paramMap) {
        Set<String> finalCols = new LinkedHashSet<>(columns);
        if (info.getIdentityColumn() != null) {
            finalCols.add(info.getIdentityColumn());
        }

        String cols = String.join(",", finalCols);
        String params = String.join(",", finalCols.stream().map(c -> "#{" + c + "}").toList());
        String insertSql = "INSERT INTO " + info.getTableName() + " (" + cols + ") VALUES (" + params + ")";

        String fullSql = dialect.buildIdentityInsert(info.getTableName(), insertSql);
        runSql(fullSql, paramMap);

        if ("postgresql".equalsIgnoreCase(dialect.getName()) && info.getIdentityColumn() != null) {
            String seqFixSql = "SELECT setval(pg_get_serial_sequence('" + info.getTableName() + "', '"
                    + info.getIdentityColumn() + "'), (SELECT COALESCE(MAX(" + info.getIdentityColumn()
                    + "), 0) FROM " + info.getTableName() + "), true)";
            queryExecutor.selectValue(seqFixSql, Map.of());
        }
    }

    /** Plain UPDATE by id column(s) - the "row already exists" branch of {@link #save}. */
    private void update(Set<String> columns, EntityInfo info, Map<String, Object> paramMap) {
        String setClause = String.join(", ", columns.stream().map(c -> c + " = #{" + c + "}").toList());
        String where = String.join(" AND ", info.getIds().stream().map(c -> c + " = #{" + c + "}").toList());
        runSql("UPDATE " + info.getTableName() + " SET " + setClause + " WHERE " + where, paramMap);
    }

    /** True if a row with this entity's id column(s) already exists (a full PK is required - returns false if any id value is missing). */
    private boolean exist(EntityInfo info, Map<String, Object> paramMap) {
        for (String id : info.getIds()) {
            if (isEmptyValue(paramMap.get(id))) {
                return false;
            }
        }

        String where = String.join(" AND ", info.getIds().stream().map(c -> c + " = #{" + c + "}").toList());
        String sql = "SELECT COUNT(*) AS total FROM " + info.getTableName() + " WHERE " + where;
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("sql", sql);
        for (String id : info.getIds()) {
            wrapper.put(id, paramMap.get(id));
        }

        Object result = session.selectOne("DynamicSQL.selectOne", wrapper);
        if (!(result instanceof Map<?, ?> row)) {
            return false;
        }
        Object total = row.get("total");
        try {
            return total != null && Integer.parseInt(total.toString()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * INSERT and hand back the DB-generated identity value, for an entity whose id is not supplied
     * by the caller. {@code insertAndReturnAutoId} is the same operation under a second name kept
     * for call-site clarity (auto-identity insert vs. an insert that merely happens to return an
     * id) - both delegate to {@link #insertReturningId}.
     */
    public Object insertAndReturnId(Set<String> columns, EntityInfo info, Map<String, Object> paramMap) {
        return insertReturningId(columns, info, paramMap);
    }

    public Object insertAndReturnAutoId(Set<String> columns, EntityInfo info, Map<String, Object> paramMap) {
        return insertReturningId(columns, info, paramMap);
    }

    /**
     * Most dialects (Postgres/MySQL/SQL Server/SQLite) embed "insert, then read the new id" in one
     * statement/round trip via {@link SqlDialect#buildInsertReturning} - see
     * {@link SqlDialect#singleStatementReturning}. Oracle can't do that through this module's
     * generic "one raw SQL string -&gt; one query" executor, so it instead provides
     * {@link SqlDialect#buildInsertReturningCallable}: a single CallableStatement round trip that
     * is still safe under concurrent inserts into the same table. Only a dialect with neither
     * option (or an entity with no identity column) falls back to the separate, non-atomic
     * {@link SqlDialect#buildIdentitySelect} query.
     */
    private Object insertReturningId(Set<String> columns, EntityInfo info, Map<String, Object> paramMap) {
        String cols = String.join(",", columns);
        String params = String.join(",", columns.stream().map(c -> "#{" + c + "}").toList());

        if (dialect.singleStatementReturning()) {
            String sql = dialect.buildInsertReturning(info.getTableName(), cols, params, info.getIdentityColumn());
            return selectValue(sql, paramMap);
        }

        if (info.getIdentityColumn() != null) {
            String callableSql = dialect.buildInsertReturningCallable(info.getTableName(), cols, params, info.getIdentityColumn());
            if (callableSql != null) {
                return runCallableReturningId(callableSql, paramMap);
            }
        }

        // Last-resort fallback: separate INSERT + best-effort SELECT - NOT safe under concurrent
        // inserts into the same table (see SqlDialect#buildIdentitySelect). Not exercised by any
        // current dialect when an identity column is present (Oracle always has a callable path).
        String sql = dialect.buildInsertReturning(info.getTableName(), cols, params, info.getIdentityColumn());
        runSql(sql, paramMap);
        if (info.getIdentityColumn() == null) {
            return null;
        }
        String selectSql = dialect.buildIdentitySelect(info.getTableName(), info.getIdentityColumn());
        return selectValue(selectSql, Map.of());
    }

    /**
     * Runs {@code sql} (an anonymous PL/SQL RETURNING ... INTO block, for Oracle) as a
     * CallableStatement via the generic {@code DynamicSQL.executeCallable} mapper statement, then
     * reads back the identity value MyBatis bound into {@link SqlDialect#RETURNING_ID_PARAM} on the
     * parameter map after execution.
     */
    private Object runCallableReturningId(String sql, Map<String, Object> paramMap) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("sql", sql);
        wrapper.putAll(paramMap);
        wrapper.put(SqlDialect.RETURNING_ID_PARAM, null);
        session.update("DynamicSQL.executeCallable", wrapper);
        return wrapper.get(SqlDialect.RETURNING_ID_PARAM);
    }

    /** Runs {@code sql} and unwraps a single scalar result from the first column of the first (only) returned row. */
    private Object selectValue(String sql, Map<String, Object> params) {
        Object result = queryExecutor.selectValue(sql, params);
        if (result instanceof Map<?, ?> row) {
            return row.values().stream().findFirst().orElse(null);
        }
        return result;
    }

    /**
     * Saves {@code entity}: INSERT (letting the DB generate an identity value), UPDATE (a row with
     * this id already exists), or INSERT with a caller-supplied key - decided per call from which
     * primary-key columns are populated and whether the entity has an identity column at all.
     * Mutates and returns the same {@code entity} instance (a generated identity value, if any, is
     * written back onto it).
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public <T> T save(T entity) {
        Class<T> clazz = (Class<T>) entity.getClass();
        EntityInfo info = EntityCache.get(clazz);
        info.populateUnaccent(entity);

        boolean isBaseEntity = entity instanceof BaseEntity;
        if (isBaseEntity) {
            BaseEntity.populateInsertAudit((BaseEntity) entity);
        }

        Map<String, Object> paramMap = new LinkedHashMap<>();
        Set<String> columns = new LinkedHashSet<>();

        for (Map.Entry<String, String> e : info.getColumns().entrySet()) {
            String col = e.getKey();
            String fieldName = e.getValue();
            Field field = info.getFieldMap().get(col);
            Object value;
            try {
                value = field != null ? field.get(entity) : FieldUtils.readField(entity, fieldName, true);
            } catch (Exception ex) {
                try {
                    value = FieldUtils.readField(entity, fieldName, true);
                } catch (Exception ex2) {
                    throw new IllegalStateException("Cannot read field " + fieldName + " on " + clazz.getName(), ex2);
                }
            }
            paramMap.put(col, value);
            columns.add(col);
        }

        if (info.getIds().isEmpty()) {
            throw new IllegalStateException("Entity " + clazz.getName() + " has no @Id field");
        }

        List<String> primaryKeys = List.copyOf(info.getIds());
        String identityCol = info.getIdentityColumn();
        Map<String, Object> pkValues = new LinkedHashMap<>();
        for (String pk : primaryKeys) {
            pkValues.put(pk, paramMap.get(pk));
        }

        boolean anyPkMissing = primaryKeys.stream().anyMatch(pk -> isEmptyValue(pkValues.get(pk)));
        boolean identityProvided = identityCol != null && !isEmptyValue(pkValues.get(identityCol));
        boolean missingNonIdentityPk = primaryKeys.stream()
                .anyMatch(pk -> !pk.equals(identityCol) && isEmptyValue(pkValues.get(pk)));

        if (missingNonIdentityPk) {
            throw new IllegalStateException("Missing non-identity primary key for " + clazz.getName());
        }

        // CASE 1: PK missing -> auto identity insert.
        if (anyPkMissing && identityCol != null) {
            Set<String> insertCols = new LinkedHashSet<>(columns);
            insertCols.remove(identityCol);
            Object newId = insertAndReturnAutoId(insertCols, info, filterKeys(paramMap, insertCols));
            if (newId != null) writeId(entity, info, newId);
            return entity;
        }

        // Missing PK but no identity -> error.
        if (anyPkMissing) {
            throw new IllegalStateException("Missing primary key for non-identity entity: " + clazz.getName());
        }

        // CASE 2: UPDATE if the row already exists.
        if (exist(info, paramMap)) {
            Set<String> updateCols = new LinkedHashSet<>(columns);
            updateCols.removeAll(primaryKeys);
            if (isBaseEntity) {
                BaseEntity base = (BaseEntity) entity;
                BaseEntity.populateUpdateAudit(base);

                String updatedAtCol = info.getFieldColumns().get("updatedAt");
                if (updatedAtCol != null) {
                    paramMap.put(updatedAtCol, base.getUpdatedAt());
                }
                String updatedByCol = info.getFieldColumns().get("updatedBy");
                if (updatedByCol != null) {
                    paramMap.put(updatedByCol, base.getUpdatedBy());
                }

                // Protect createdAt and createdBy from being overwritten on UPDATE
                String createdAtCol = info.getFieldColumns().get("createdAt");
                if (createdAtCol != null) {
                    updateCols.remove(createdAtCol);
                }
                String createdByCol = info.getFieldColumns().get("createdBy");
                if (createdByCol != null) {
                    updateCols.remove(createdByCol);
                }
            }
            update(updateCols, info, paramMap);
            return entity;
        }

        // CASE 3: manual identity insert (caller supplied the identity value themselves).
        if (identityCol != null && identityProvided) {
            insertWithIdentity(columns, info, paramMap);
            writeId(entity, info, paramMap.get(identityCol));
            return entity;
        }

        // CASE 4: composite PK, no identity column.
        if (identityCol == null && primaryKeys.size() > 1) {
            insert(columns, info, paramMap);
            return entity;
        }

        // CASE 5: auto identity via INSERT ... RETURNING - skip the identity column if it's empty.
        Set<String> safeInsertCols = new LinkedHashSet<>(columns);
        if (identityCol != null && isEmptyValue(pkValues.get(identityCol))) {
            safeInsertCols.remove(identityCol);
        }

        Object newId = insertAndReturnId(safeInsertCols, info, filterKeys(paramMap, safeInsertCols));
        if (newId != null && identityCol != null) writeId(entity, info, newId);

        return entity;
    }

    /** Copies just {@code keys} out of {@code map}, in {@code keys}' iteration order. */
    private Map<String, Object> filterKeys(Map<String, Object> map, Set<String> keys) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String k : keys) {
            result.put(k, map.get(k));
        }
        return result;
    }
}
