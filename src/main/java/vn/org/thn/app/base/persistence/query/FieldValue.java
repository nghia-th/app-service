package vn.org.thn.app.base.persistence.query;


import vn.org.thn.app.base.persistence.lambda.SFunction;

/**
 * One field/value pair for composite-key lookups, built via {@link #of}. Java has no built-in
 * Pair, so this stands in for the Kotlin original's Pair<KProperty1<T,*>, Any?> varargs used by
 * BaseRepository#findByIds/existsByIds/deleteByIds.
 */
public final class FieldValue<T> {

    private final SFunction<T, ?> field;
    private final Object value;

    private FieldValue(SFunction<T, ?> field, Object value) {
        this.field = field;
        this.value = value;
    }

    /** Creates one field/value pair, e.g. {@code FieldValue.of(User::getId, 42)}. */
    public static <T> FieldValue<T> of(SFunction<T, ?> field, Object value) {
        return new FieldValue<>(field, value);
    }

    /** The field accessor supplied to {@link #of}. */
    public SFunction<T, ?> field() {
        return field;
    }

    /** The value supplied to {@link #of}. */
    public Object value() {
        return value;
    }
}
