package vn.org.thn.app.base.persistence.repository;

import vn.org.thn.app.base.persistence.query.DeleteBuilder;
import vn.org.thn.app.base.persistence.query.FieldValue;
import vn.org.thn.app.base.persistence.query.QueryBuilder;
import vn.org.thn.app.base.persistence.query.UpdateBuilder;

import java.util.Collection;
import java.util.List;

/**
 * Generic CRUD + query-DSL contract every entity repository implements, by extending
 * {@link BaseRepositoryImpl}. Ported from the Kotlin original's {@code BaseRepository<T, ID>}
 * (its {@code KProperty1}-based vararg id-pairs become {@link FieldValue}, the Java stand-in for
 * Kotlin's {@code Pair}).
 */
public interface BaseRepository<T, ID> {

    /** Inserts or updates {@code entity} (INSERT/UPDATE decided per the rules in {@link vn.org.thn.service.base.db.mybatis.executor.InsertExecutor#save}). Returns the same instance, mutated with any generated id. */
    T save(T entity);

    /** Bulk-saves every entity in {@code entities}. See {@link vn.org.thn.service.base.db.mybatis.executor.BatchInsertExecutor#saveAll} for the identity-column vs. flattened-batch split. */
    List<T> saveAll(Collection<T> entities);

    /** Deletes the row with primary key {@code id}. Requires a single-column primary key - use {@link #deleteByIds} for a composite key. */
    int deleteById(ID id);

    /** Finds the row with primary key {@code id}, or null if none exists. Requires a single-column primary key - use {@link #findByIds} for a composite key. */
    T findById(ID id);

    /** Whether a row with primary key {@code id} exists. Requires a single-column primary key - use {@link #existsByIds} for a composite key. */
    boolean existsById(ID id);

    /** Total row count for the entity's table. */
    long count();

    /** Every row of the entity's table, unfiltered. */
    List<T> findAll();

    /** A fresh {@link QueryBuilder} for this entity, the entry point for filtered/paged/aggregate SELECTs. */
    QueryBuilder<T> query();

    /** A fresh {@link UpdateBuilder} for this entity. */
    UpdateBuilder<T> update();

    /** A fresh {@link DeleteBuilder} for this entity. */
    DeleteBuilder<T> delete();

    /** Finds the row matching every given field/value pair (AND-joined) - for composite-key or multi-field lookups. Returns null if none matches. */
    @SuppressWarnings("unchecked")
    T findByIds(FieldValue<T>... ids);

    /** Whether a row matches every given field/value pair (AND-joined). */
    @SuppressWarnings("unchecked")
    boolean existsByIds(FieldValue<T>... ids);

    /** Deletes every row matching every given field/value pair (AND-joined). Returns the affected-row count. */
    @SuppressWarnings("unchecked")
    int deleteByIds(FieldValue<T>... ids);
}
