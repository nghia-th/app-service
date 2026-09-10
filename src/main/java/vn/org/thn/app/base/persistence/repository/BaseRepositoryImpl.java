package vn.org.thn.app.base.persistence.repository;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import vn.org.thn.app.base.persistence.executor.*;
import vn.org.thn.app.base.persistence.query.*;
import vn.org.thn.app.base.persistence.executor.*;
import vn.org.thn.app.base.persistence.metadata.EntityCache;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;
import vn.org.thn.app.base.persistence.query.*;


import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Base implementation of {@link BaseRepository}. Subclass with concrete type arguments and
 * register as a Spring bean:
 * <pre>{@code
 * @Repository
 * public class TranslateRepository extends BaseRepositoryImpl<Translate, Long> {
 * }
 * }</pre>
 * The entity class is recovered once via the generic superclass's type argument (same reflection
 * trick as the Kotlin original), and the executor beans are field-injected with {@code @Autowired}
 * - simpler than the original's {@code ApplicationContextProvider}-based lazy service-locator
 * lookups: a plain {@code @Autowired} field on an abstract Spring-managed superclass is wired
 * normally by Spring, no service locator needed in Java.
 */
public abstract class BaseRepositoryImpl<T, ID> implements BaseRepository<T, ID> {

    @Autowired
    protected QueryExecutor queryExecutor;

    @Autowired
    protected InsertExecutor insertExecutor;

    @Autowired
    protected MapperExecutor mapperExecutor;

    @Autowired
    protected NativeQueryExecutor nativeQueryExecutor;

    @Autowired
    protected BatchInsertExecutor batchInsertExecutor;

    private volatile Class<T> entityClass;
    private volatile EntityInfo entityInfo;

    /** Resolves and caches this repository's entity class from its own generic superclass's type argument (computed once, lazily, per subclass instance). */
    @SuppressWarnings("unchecked")
    protected Class<T> entityClass() {
        Class<T> result = entityClass;
        if (result == null) {
            ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
            result = (Class<T>) genericSuperclass.getActualTypeArguments()[0];
            entityClass = result;
        }
        return result;
    }

    /** This repository's cached {@link EntityInfo} (looked up via {@link EntityCache} on first use). */
    protected EntityInfo entityInfo() {
        EntityInfo result = entityInfo;
        if (result == null) {
            result = EntityCache.get(entityClass());
            entityInfo = result;
        }
        return result;
    }

    /** The entity's single primary-key column name. Throws {@link IllegalStateException} for a composite key. */
    protected String idColumn() {
        if (entityInfo().getIds().size() != 1) {
            throw new IllegalStateException("Composite primary key not supported by idColumn()");
        }
        return entityInfo().getIds().iterator().next();
    }

    @Override
    public T save(T entity) {
        return insertExecutor.save(entity);
    }

    @Override
    public List<T> saveAll(Collection<T> entities) {
        return batchInsertExecutor.saveAll(entities);
    }

    @Override
    public QueryBuilder<T> query() {
        return new QueryBuilder<>(entityClass(), entityInfo(), queryExecutor);
    }

    @Override
    public UpdateBuilder<T> update() {
        return new UpdateBuilder<>(entityClass(), entityInfo(), queryExecutor);
    }

    @Override
    public DeleteBuilder<T> delete() {
        return new DeleteBuilder<>(entityClass(), entityInfo(), queryExecutor);
    }

    @Override
    public long count() {
        return query().count();
    }

    @Override
    public List<T> findAll() {
        return query().list();
    }

    @Override
    public boolean existsById(ID id) {
        if (entityInfo().getIds().size() == 1) {
            return query().eq(entityInfo().getIds().iterator().next(), id).exists();
        }
        return withCompositeId(query(), id).exists();
    }

    @Override
    public T findById(ID id) {
        if (entityInfo().getIds().size() == 1) {
            return query().eq(entityInfo().getIds().iterator().next(), id).one();
        }
        return withCompositeId(query(), id).one();
    }

    @Override
    public int deleteById(ID id) {
        if (entityInfo().getIds().size() == 1) {
            return delete().eq(entityInfo().getIds().iterator().next(), id).execute();
        }
        return withCompositeId(delete(), id).execute();
    }

    /** Adds one {@code eq} condition per composite-key column to {@code builder}, reading each column's value off the given composite-id object by reflection. */
    private <B extends BaseConditionBuilder<T, B>> B withCompositeId(B builder, ID id) {
        for (String column : entityInfo().getIds()) {
            String fieldName = entityInfo().getColumns().get(column);
            Object value;
            try {
                value = FieldUtils.readField(id, fieldName, true);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Cannot read composite id field " + fieldName + " on " + id.getClass().getName(), e);
            }
            builder.eq(column, value);
        }
        return builder;
    }

    /** The first row of the entity's table with no filter applied (order is whatever the database returns). */
    public T findFirst() {
        return query().first();
    }

    /** Whether the entity's table has at least one row. */
    public boolean exists() {
        return count() > 0;
    }

    /** Runs a raw SQL query, mapping each row onto {@code clazz}. See {@link NativeQueryExecutor#list}. */
    protected <R> List<R> nativeQuery(String sql, Class<R> clazz, Map<String, Object> params) {
        return nativeQueryExecutor.list(sql, clazz, params);
    }

    /** Runs a raw SQL query and returns the first row mapped onto {@code clazz}, or null if none matched. */
    protected <R> R nativeQueryOne(String sql, Class<R> clazz, Map<String, Object> params) {
        return nativeQueryExecutor.one(sql, clazz, params);
    }

    /** Runs a raw SQL query and returns every row as a column-name -> value map. */
    protected List<Map<String, Object>> nativeQueryMap(String sql, Map<String, Object> params) {
        return nativeQueryExecutor.listMap(sql, params);
    }

    /** Runs a raw SQL query and returns the first column of the first row (e.g. a scalar aggregate). */
    protected Object nativeValue(String sql, Map<String, Object> params) {
        return nativeQueryExecutor.value(sql, params);
    }

    @Override
    @SuppressWarnings({"unchecked", "varargs"})
    public T findByIds(FieldValue<T>... ids) {
        QueryBuilder<T> builder = query();
        for (FieldValue<T> id : ids) {
            builder.eq(id.field(), castValue(id));
        }
        return builder.one();
    }

    @Override
    @SuppressWarnings({"unchecked", "varargs"})
    public boolean existsByIds(FieldValue<T>... ids) {
        QueryBuilder<T> builder = query();
        for (FieldValue<T> id : ids) {
            builder.eq(id.field(), castValue(id));
        }
        return builder.exists();
    }

    @Override
    @SuppressWarnings({"unchecked", "varargs"})
    public int deleteByIds(FieldValue<T>... ids) {
        DeleteBuilder<T> builder = delete();
        for (FieldValue<T> id : ids) {
            builder.eq(id.field(), castValue(id));
        }
        return builder.execute();
    }

    /** Unchecked cast of one {@link FieldValue}'s value to the type its field setter expects. */
    @SuppressWarnings("unchecked")
    private static <R> R castValue(FieldValue<?> id) {
        return (R) id.value();
    }

    /** Looks up a MyBatis mapper interface bean by type. See {@link MapperExecutor#mapper}. */
    protected <M> M mapper(Class<M> mapperInterface) {
        return mapperExecutor.mapper(mapperInterface);
    }
}
