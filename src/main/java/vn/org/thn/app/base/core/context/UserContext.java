package vn.org.thn.app.base.core.context;

/**
 * Thread-local context holder for the current authenticated user / actor.
 * Used by {@link vn.org.thn.app.base.persistence.executor.InsertExecutor}
 * to automatically populate audit fields ({@code createdBy}, {@code updatedBy}).
 */
public final class UserContext {

    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * Gets the username / user ID of the current authenticated user.
     *
     * @return current username/id or {@code null} if not set.
     */
    public static String getCurrentUser() {
        return CURRENT_USER.get();
    }

    /**
     * Sets the current username / user ID for the calling thread.
     *
     * @param username current user identifier
     */
    public static void setCurrentUser(String username) {
        if (username != null && !username.isBlank()) {
            CURRENT_USER.set(username.trim());
        } else {
            CURRENT_USER.remove();
        }
    }

    /**
     * Clears the current user context for the calling thread.
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
