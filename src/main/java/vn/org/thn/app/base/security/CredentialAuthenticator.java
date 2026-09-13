package vn.org.thn.app.base.security;

/**
 * Verifies a username/password pair against whatever user store a consuming service actually has,
 * and hands back the identity + authorities to embed in a token. {@code base} deliberately knows
 * nothing about any specific user table shape (that's domain-specific - e.g. {@code app-service}'s
 * own {@code UserEntity}), so a service running {@link JwtMode#STANDALONE} must provide exactly one
 * bean implementing this interface (e.g. {@code app-service}'s {@code UserCredentialAuthenticator},
 * which wraps {@code UserRepository} and a {@code PasswordEncoder}) - without one, the login
 * endpoint has nothing to check credentials against and the application context fails to start.
 * <p>
 * Not needed at all in {@link JwtMode#RESOURCE_SERVER} mode, since that mode never exposes a login
 * endpoint - a separate auth microservice owns credential verification entirely.
 */
public interface CredentialAuthenticator {

    /**
     * @return the authenticated principal, or {@code null} if the username doesn't exist, the
     * password doesn't match, or the account isn't allowed to log in right now (e.g. inactive) -
     * any of these should read as "invalid credentials" to the caller, never a different error, so
     * a client can't distinguish "wrong password" from "no such user" (a standard measure against
     * username enumeration).
     */
    AuthenticatedPrincipal authenticate(String username, String rawPassword);
}
