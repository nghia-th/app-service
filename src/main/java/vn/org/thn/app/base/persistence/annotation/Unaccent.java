package vn.org.thn.app.base.persistence.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity field as an unaccented/normalized representation of another field.
 * <p>
 * During {@code save()}, {@code insert()}, or {@code update()} operations, the framework
 * automatically reads the string value from {@link #from()}, converts it using
 * {@link vn.org.thn.app.base.util.StringUtils#toUnaccent(String)}, and writes the unaccented
 * result to the annotated field before generating SQL statements.
 * <p>
 * Example:
 * <pre>{@code
 *   @Column(name = "full_name")
 *   private String fullName;
 *
 *   @Unaccent(from = "fullName")
 *   @Column(name = "full_name_unaccent")
 *   private String fullNameUnaccent;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Unaccent {

    /** The name of the source field on the entity whose value should be unaccented. */
    String from();
}
