package vn.org.thn.app.base.persistence.executor;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.app.base.persistence.dialect.SqlDialect;
import vn.org.thn.app.base.persistence.metadata.EntityCache;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactional bulk save: falls back to {@code InsertExecutor#save} per row when the entity has
 * an identity column (needs a real INSERT-and-read-back per row to capture the generated key, so
 * there's no way around one statement per row there), otherwise builds flattened multi-row
 * {@code INSERT ... VALUES (...),(...),...} statements, chunked to the dialect's
 * {@link SqlDialect#maxBatchRows(int)} the same way {@link BatchExecutor} does.
 * <p>
 * The Kotlin original built the VALUES list with embedded {@code #{list[i].field}} placeholders
 * bound via MyBatis OGNL navigating straight into each entity's getters (works for Kotlin data
 * classes, not guaranteed for a plain Java field-based entity) and issued one unchunked statement
 * for the whole collection regardless of size. This port avoids the getter dependency by
 * flattening values into an explicit parameter map up front - same reflection metadata
 * ({@code EntityInfo#getFieldMap()}) the rest of the ORM already uses - and chunks large
 * collections to stay under each engine's bound-parameter limit.
 */
@Component
public class BatchInsertExecutor {

    private final SqlSessionTemplate session;
    private final SqlDialect dialect;
    private final InsertExecutor insertExecutor;

    public BatchInsertExecutor(SqlSessionTemplate session, SqlDialect dialect, InsertExecutor insertExecutor) {
        this.session = session;
        this.dialect = dialect;
        this.insertExecutor = insertExecutor;
    }

    /** Saves every entity in {@code entities}, returning them (mutated in place with any generated ids). See the class doc for the identity-column vs. flattened-batch split. */
    @Transactional
    @SuppressWarnings("unchecked")
    public <T> List<T> saveAll(Collection<T> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }

        Class<T> clazz = (Class<T>) entities.iterator().next().getClass();
        EntityInfo info = EntityCache.get(clazz);

        if (info.getIdentityColumn() != null) {
            List<T> saved = new ArrayList<>(entities.size());
            for (T entity : entities) {
                saved.add(insertExecutor.save(entity));
            }
            return saved;
        }

        List<T> entityList = new ArrayList<>(entities);
        List<String> columns = new ArrayList<>(info.getColumns().keySet());
        if (columns.isEmpty()) {
            throw new IllegalStateException("No insert columns found");
        }

        int maxRowsPerBatch = dialect.maxBatchRows(columns.size());

        for (int from = 0; from < entityList.size(); from += maxRowsPerBatch) {
            int to = Math.min(from + maxRowsPerBatch, entityList.size());
            insertChunk(entityList.subList(from, to), info, columns, clazz);
        }

        return entityList;
    }

    /** Builds and runs one multi-row INSERT statement for one chunk of entities. */
    private <T> void insertChunk(List<T> chunk, EntityInfo info, List<String> columns, Class<T> clazz) {
        StringBuilder sql = new StringBuilder(1024);
        sql.append("INSERT INTO ").append(info.getTableName()).append(" (")
                .append(String.join(",", columns)).append(") VALUES ");

        Map<String, Object> params = new HashMap<>((chunk.size() + 2) * columns.size());

        for (int rowIdx = 0; rowIdx < chunk.size(); rowIdx++) {
            T entity = chunk.get(rowIdx);
            sql.append('(');
            for (int cIdx = 0; cIdx < columns.size(); cIdx++) {
                String col = columns.get(cIdx);
                Object value = readColumnValue(entity, info, col, clazz);
                String key = "p" + rowIdx + "_" + cIdx;
                params.put(key, value);
                sql.append("#{").append(key).append('}');
                if (cIdx < columns.size() - 1) sql.append(',');
            }
            sql.append(')');
            if (rowIdx < chunk.size() - 1) sql.append(',');
        }

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("sql", sql.toString());
        wrapper.putAll(params);
        session.update("DynamicSQL.execute", wrapper);
    }

    /** Reads one entity's value for {@code col} (direct field access first, {@link FieldUtils} as fallback). */
    private <T> Object readColumnValue(T entity, EntityInfo info, String col, Class<T> clazz) {
        Field field = info.getFieldMap().get(col);
        try {
            if (field != null) {
                return field.get(entity);
            }
        } catch (Exception ignored) {
            // fall through to FieldUtils
        }
        try {
            return FieldUtils.readField(entity, info.getColumns().get(col), true);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read field for column " + col + " on " + clazz.getName(), e);
        }
    }
}
