package vn.org.thn.app.base.persistence.executor;

import org.apache.commons.lang3.reflect.FieldUtils;
import vn.org.thn.app.base.persistence.metadata.EntityCache;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;
import vn.org.thn.app.base.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps a raw JDBC result row (column name -&gt; value, as produced by mapper/DynamicSQL.xml's
 * generic {@code resultType="map"} statements) onto a plain Java class via reflection.
 * <p>
 * This replaces the Kotlin original's approach of running every row through a Jackson
 * {@code ObjectMapper#convertValue(row, clazz)} configured with
 * {@code PropertyNamingStrategies.SNAKE_CASE}. Deliberately not reusing {@link vn.org.thn.app.base.util.JsonUtils}
 * here: this has nothing to do with JSON, and after the JsonUtils/Jackson-3 package-path debugging
 * earlier in this port, row-mapping is kept on the same plain-reflection metadata the rest of the
 * ORM already uses (entity classes reuse {@link EntityCache}; anything else gets an ad-hoc
 * snake_case-derived column map, cached the same way).
 */
final class RowMapper {

    /** Column-name -> Field maps for non-entity target classes (DTOs, projections), keyed by class and cached the same way {@link EntityCache} caches entities. */
    private static final Map<Class<?>, Map<String, Field>> AD_HOC_CACHE = new ConcurrentHashMap<>();

    private RowMapper() {
    }

    /** Maps one row onto a new {@code clazz} instance (or returns the row itself unchanged if {@code clazz} is a {@link Map} type). */
    @SuppressWarnings("unchecked")
    static <T> T map(Map<String, Object> row, Class<T> clazz) {
        if (Map.class.isAssignableFrom(clazz)) {
            return (T) row;
        }

        Map<String, Field> columnToField = fieldsForClass(clazz);

        T instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot map query result to " + clazz.getName() + ": no accessible no-arg constructor", e);
        }

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String column = entry.getKey() == null ? null : entry.getKey().toLowerCase();
            Field field = column == null ? null : columnToField.get(column);
            if (field == null) {
                continue;
            }
            Object value = convert(entry.getValue(), field.getType());
            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot set field " + field.getName() + " on " + clazz.getName(), e);
            }
        }

        return instance;
    }

    /** Column-name -> Field map for {@code clazz}: {@link EntityCache}'s metadata if it's a mapped entity, else a cached ad-hoc snake_case-derived map. */
    private static Map<String, Field> fieldsForClass(Class<?> clazz) {
        EntityInfo entityInfo = tryEntity(clazz);
        if (entityInfo != null) {
            return entityInfo.getFieldMap();
        }
        return AD_HOC_CACHE.computeIfAbsent(clazz, RowMapper::buildAdHocMap);
    }

    /** {@link EntityCache#get} without throwing when {@code clazz} isn't a mapped entity. */
    private static EntityInfo tryEntity(Class<?> clazz) {
        try {
            return EntityCache.get(clazz);
        } catch (IllegalArgumentException notAnEntity) {
            return null;
        }
    }

    /** Builds a column-name -> Field map for a non-entity class by snake-casing every non-static field's name. */
    private static Map<String, Field> buildAdHocMap(Class<?> clazz) {
        Map<String, Field> map = new LinkedHashMap<>();
        List<Field> fields = FieldUtils.getAllFieldsList(clazz);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            String column = StringUtils.camelToSnake(field.getName());
            map.put(column, field);
        }
        return map;
    }

    /** Converts one JDBC value to the target field's declared type (numeric widening, java.sql.* -> java.time.*, enum-by-name, string coercion, ...); returns the value unchanged if no known conversion applies. */
    @SuppressWarnings("unchecked")
    private static Object convert(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType.isEnum() && value instanceof String s) {
            return Enum.valueOf((Class<Enum>) targetType, s);
        }

        if (value instanceof Number number) {
            if (targetType == Integer.class || targetType == int.class) return number.intValue();
            if (targetType == Long.class || targetType == long.class) return number.longValue();
            if (targetType == Double.class || targetType == double.class) return number.doubleValue();
            if (targetType == Float.class || targetType == float.class) return number.floatValue();
            if (targetType == Short.class || targetType == short.class) return number.shortValue();
            if (targetType == Byte.class || targetType == byte.class) return number.byteValue();
            if (targetType == BigDecimal.class) return new BigDecimal(number.toString());
            if (targetType == BigInteger.class) return BigInteger.valueOf(number.longValue());
            if (targetType == Boolean.class || targetType == boolean.class) return number.intValue() != 0;
        }

        if (value instanceof Timestamp ts) {
            if (targetType == LocalDateTime.class) return ts.toLocalDateTime();
            if (targetType == LocalDate.class) return ts.toLocalDateTime().toLocalDate();
            if (targetType == Instant.class) return ts.toInstant();
            if (targetType == Long.class || targetType == long.class) return ts.getTime();
        }

        if (value instanceof Date date) {
            if (targetType == LocalDate.class) return date.toLocalDate();
            if (targetType == LocalDateTime.class) return date.toLocalDate().atStartOfDay();
        }

        if (value instanceof Time time) {
            if (targetType == LocalTime.class) return time.toLocalTime();
        }

        if (value instanceof String s) {
            if (targetType == LocalDateTime.class) return parseLocalDateTime(s);
            if (targetType == LocalDate.class) return parseLocalDate(s);
            if (targetType == LocalTime.class) return LocalTime.parse(s.trim());
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(s) || "1".equals(s);
        }

        return value;
    }

    /**
     * Parses a String -&gt; LocalDateTime as leniently as practical: a DB driver's exact text for a
     * datetime-ish TEXT/VARCHAR column isn't guaranteed to be plain ISO-8601 - a space instead of
     * "T" between date and time is the common case (handled first, same as before), but a trailing
     * UTC/offset marker or missing seconds can also show up depending on how the row was written -
     * see Medium finding #9 (2026-09-12 review). A trailing "Z" or numeric offset (e.g. "+07:00") is
     * stripped before parsing, since {@link LocalDateTime} has no timezone component to hold it.
     * Throws {@link IllegalStateException} naming the raw value - instead of a bare
     * {@link DateTimeParseException} pointing only at java.time internals - when every attempt
     * fails, so a genuinely unexpected format is easier to diagnose from the log.
     */
    private static LocalDateTime parseLocalDateTime(String raw) {
        String s = raw.trim().replace(' ', 'T').replaceAll("(Z|[+-]\\d{2}:?\\d{2})$", "");
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            try {
                // Missing seconds, e.g. "2024-01-01T10:00".
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (DateTimeParseException e2) {
                throw new IllegalStateException(
                        "Cannot parse '" + raw + "' as LocalDateTime (unexpected DB text format)", e2);
            }
        }
    }

    /** Same leniency/error-reporting approach as {@link #parseLocalDateTime}, for a plain date column. */
    private static LocalDate parseLocalDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(
                    "Cannot parse '" + raw + "' as LocalDate (unexpected DB text format)", e);
        }
    }
}
