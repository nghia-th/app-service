package vn.org.thn.app.base.persistence.lambda;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a getter method reference (e.g. {@code Translate::getLangKey}) to the entity field
 * name it reads (e.g. {@code "langKey"}) - the "MyBatis-Plus LambdaQueryWrapper" trick: every
 * {@link SFunction} handed to the query DSL is a {@link Serializable} lambda, and the JVM lets
 * us call its private {@code writeReplace()} to get a {@link SerializedLambda} describing the
 * real implementation method. {@code getImplMethodName()} is the getter name ("getLangKey" /
 * "isActive"), which is turned into the field name by stripping the "get"/"is" prefix and
 * decapitalizing the rest.
 * <p>
 * Resolution needs reflection plus a throwaway class load per distinct lambda shape, so results
 * are cached keyed by implementation class + method - one entry per method reference used
 * anywhere in the app, filled once and reused for the life of the JVM.
 */
public final class LambdaFieldResolver {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private LambdaFieldResolver() {
    }

    /** Resolves {@code fn} to the field name its underlying getter reads, using the cache when this exact implementation method has been resolved before. Throws if {@code fn} is null or not a plain getter reference. */
    public static <T, R> String resolve(SFunction<T, R> fn) {
        if (fn == null) {
            throw new IllegalArgumentException("field reference must not be null");
        }
        SerializedLambda serialized = extractLambda(fn);
        String key = serialized.getImplClass() + "#" + serialized.getImplMethodName();
        return CACHE.computeIfAbsent(key, k -> methodNameToFieldName(serialized.getImplMethodName()));
    }

    /** Invokes the lambda's synthetic {@code writeReplace()} to obtain the {@link SerializedLambda} describing which method it points to. */
    private static SerializedLambda extractLambda(Serializable fn) {
        try {
            Method writeReplace = fn.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object replacement = writeReplace.invoke(fn);
            if (replacement instanceof SerializedLambda serializedLambda) {
                return serializedLambda;
            }
            throw new IllegalStateException("Not a serialized lambda: " + fn.getClass());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot resolve field name from method reference " + fn.getClass()
                            + " - make sure it is a plain getter reference, e.g. Entity::getField", e);
        }
    }

    /** Strips a JavaBean getter prefix ({@code get}/{@code is}) and decapitalizes the rest, e.g. {@code "getLangKey"} -> {@code "langKey"}. Returns {@code methodName} unchanged if it doesn't look like a getter. */
    private static String methodNameToFieldName(String methodName) {
        String name = methodName;
        if (name.startsWith("get") && name.length() > 3) {
            name = name.substring(3);
        } else if (name.startsWith("is") && name.length() > 2) {
            name = name.substring(2);
        } else {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
