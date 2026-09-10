package vn.org.thn.app.base.persistence.lambda;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A method reference that is also {@link Serializable}, which lets the JVM hand back a
 * {@link java.lang.invoke.SerializedLambda} describing which method it points to.
 * <p>
 * This is the Java stand-in for Kotlin's {@code KProperty1<T, R>}: instead of writing
 * {@code Translate::langKey}, entities are queried with {@code Translate::getLangKey}, and
 * {@link LambdaFieldResolver} turns that getter reference back into the field name
 * {@code "langKey"} at runtime (resolved once per distinct method reference, then cached).
 * Requires the entity to expose a standard JavaBean getter for the field being referenced.
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
}
