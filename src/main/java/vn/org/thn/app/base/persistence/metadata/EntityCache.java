package vn.org.thn.app.base.persistence.metadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** One-time-parsed-per-class cache in front of {@link EntityParser}, since reflection is not free. */
public final class EntityCache {

    private static final Map<Class<?>, EntityInfo> CACHE = new ConcurrentHashMap<>();

    private EntityCache() {
    }

    /** Returns {@code clazz}'s parsed {@link EntityInfo}, parsing and caching it on first request. Throws {@link IllegalArgumentException} if {@code clazz} has no {@code @Entity} annotation. */
    public static EntityInfo get(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, c -> {
            EntityInfo info = EntityParser.parse(c);
            if (info == null) {
                throw new IllegalArgumentException(c.getName() + " is not an entity (missing @Entity)");
            }
            return info;
        });
    }

    /** Evicts {@code clazz}'s cached metadata, if present (mainly for tests that reload entity definitions). */
    public static void remove(Class<?> clazz) {
        CACHE.remove(clazz);
    }

    /** Evicts every cached entry. */
    public static void clear() {
        CACHE.clear();
    }

    /** Number of currently cached entity classes. */
    public static int size() {
        return CACHE.size();
    }
}
