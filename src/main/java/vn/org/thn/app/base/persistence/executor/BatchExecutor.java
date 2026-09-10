package vn.org.thn.app.base.persistence.executor;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.persistence.dialect.SqlDialect;
import vn.org.thn.app.base.persistence.metadata.EntityCache;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fast, identity-free bulk insert: builds one multi-row {@code INSERT INTO t (...) VALUES (...),(...),...}
 * per batch (chunked to the dialect's {@link SqlDialect#maxBatchRows(int)} to stay under each
 * engine's bound-parameter limit, SQL Server in particular), with values flattened into a plain
 * parameter map (keys {@code p<row>_<col>}) up front - no per-row map allocation, no reliance on
 * MyBatis OGNL navigating into entity getters.
 */
@Component
public class BatchExecutor {

    private final SqlSessionTemplate session;
    private final SqlDialect dialect;

    public BatchExecutor(SqlSessionTemplate session, SqlDialect dialect) {
        this.session = session;
        this.dialect = dialect;
    }

    /**
     * Inserts every entity in {@code list}, chunked to the dialect's batch-row limit. Rejects an
     * entity type with an identity column - a generated id can't be captured from a flattened
     * multi-row INSERT, so use {@link BatchInsertExecutor#saveAll} or {@link InsertExecutor#save}
     * for those instead.
     */
    @SuppressWarnings("unchecked")
    public <T> void insert(List<T> list) {
        if (list.isEmpty()) return;

        Class<T> clazz = (Class<T>) list.get(0).getClass();
        EntityInfo info = EntityCache.get(clazz);

        if (info.getIdentityColumn() != null) {
            throw new IllegalStateException(
                    "Fast batch insert does not support an entity with an identity column "
                            + "(use BatchInsertExecutor#saveAll or InsertExecutor#save instead).");
        }

        List<String> columns = new ArrayList<>(info.getColumns().keySet());
        int colCount = columns.size();
        if (colCount == 0) {
            throw new IllegalStateException("No columns found for " + clazz.getName());
        }

        int maxRowsPerBatch = dialect.maxBatchRows(colCount);
        String table = info.getTableName();
        if (table == null) {
            throw new IllegalStateException("Table name missing for " + clazz.getName());
        }

        List<Object> valueBuffer = new ArrayList<>(maxRowsPerBatch * colCount);
        int rowsInBatch = 0;
        int globalRowIndex = 0;

        for (T entity : list) {
            for (String col : columns) {
                Field field = info.getFieldMap().get(col);
                Object value;
                try {
                    value = field != null ? field.get(entity) : FieldUtils.readField(entity, info.getColumns().get(col), true);
                } catch (Exception e) {
                    try {
                        value = FieldUtils.readField(entity, info.getColumns().get(col), true);
                    } catch (Exception e2) {
                        throw new IllegalStateException("Cannot read field for column " + col + " on " + clazz.getName(), e2);
                    }
                }
                valueBuffer.add(value);
            }
            rowsInBatch++;

            if (rowsInBatch >= maxRowsPerBatch) {
                flushBatch(table, columns, valueBuffer, rowsInBatch, globalRowIndex);
                globalRowIndex += rowsInBatch;
                valueBuffer.clear();
                rowsInBatch = 0;
            }
        }

        if (rowsInBatch > 0) {
            flushBatch(table, columns, valueBuffer, rowsInBatch, globalRowIndex);
        }
    }

    /** Builds and runs one multi-row INSERT statement for {@code rowsInBatch} rows' worth of values already collected in {@code valueBuffer}. */
    private void flushBatch(String table, List<String> columns, List<Object> valueBuffer, int rowsInBatch, int globalRowIndex) {
        StringBuilder sql = new StringBuilder(1024);
        sql.append("INSERT INTO ").append(table).append(" (").append(String.join(",", columns)).append(") VALUES ");

        Map<String, Object> params = new HashMap<>((rowsInBatch + 2) * columns.size());

        for (int rowIdx = 0; rowIdx < rowsInBatch; rowIdx++) {
            sql.append('(');
            for (int cIdx = 0; cIdx < columns.size(); cIdx++) {
                String key = "p" + (globalRowIndex + rowIdx) + "_" + cIdx;
                sql.append("#{").append(key).append('}');
                if (cIdx < columns.size() - 1) sql.append(',');
            }
            sql.append(')');
            if (rowIdx < rowsInBatch - 1) sql.append(',');
        }

        int vbIndex = 0;
        for (int rowIdx = 0; rowIdx < rowsInBatch; rowIdx++) {
            for (int cIdx = 0; cIdx < columns.size(); cIdx++) {
                String key = "p" + (globalRowIndex + rowIdx) + "_" + cIdx;
                params.put(key, valueBuffer.get(vbIndex++));
            }
        }

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("sql", sql.toString());
        wrapper.putAll(params);
        session.update("DynamicSQL.execute", wrapper);
    }
}
