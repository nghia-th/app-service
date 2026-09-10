package vn.org.thn.app.base.persistence.annotation;

/** Strategy for how a {@code @GeneratedValue} primary-key column's value is produced. */
public enum GenerationType {
    /** Let the engine pick its native strategy (currently treated the same as {@link #IDENTITY} by {@code EntityParser}). */
    AUTO,
    /** Database-generated identity/auto-increment column. */
    IDENTITY,
    /** Application-generated UUID (not auto-populated by {@code EntityParser}; the caller supplies the value). */
    UUID
}
