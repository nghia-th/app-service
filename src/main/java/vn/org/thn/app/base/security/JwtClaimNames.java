package vn.org.thn.app.base.security;

/**
 * Names of the custom JWT claims that both {@link JwtMode#STANDALONE} and
 * {@link JwtMode#RESOURCE_SERVER} agree on - this is the "wire contract" between a token issuer
 * (this service in standalone mode, or a separate auth microservice in resource-server mode) and
 * every service that verifies the token.
 * <p>
 * Beyond these, a token is expected to carry the standard registered claims: {@code sub} (the
 * username - resolved to {@link vn.org.thn.app.base.core.context.UserContext} the same way
 * {@code RequestContextFilter} used to read it from a client-supplied header, except now it comes
 * from a verified signature instead of an unauthenticated header - see the 2026-09-12 review's
 * Critical finding #2), {@code iat}/{@code exp} (issued-at/expiry), and {@code iss} (issuer).
 */
public final class JwtClaimNames {

    /**
     * The authenticated user's numeric id, so a caller doesn't have to look up {@code UserEntity}
     * by username just to get it. Deliberately not the ID token's {@code sub} - {@code sub} stays
     * the human-readable username (already what {@code UserContext}/audit columns store).
     */
    public static final String USER_ID = "userId";

    /**
     * A JSON array of Spring Security authority strings (e.g. {@code ["ROLE_ADMIN"]}), not a single
     * flat role string. Every endpoint's {@code @PreAuthorize} is written explicitly (e.g.
     * {@code hasAnyRole('USER','ADMIN')} or {@code hasRole('ADMIN')}) rather than relying on a role
     * hierarchy - a token only ever carries the authorities its own role actually has (an ADMIN
     * token does not implicitly also carry {@code ROLE_USER}), so an endpoint that must stay
     * off-limits to ADMIN as well as anonymous callers can still enforce that. Using a list instead
     * of one string costs nothing extra to implement now (Spring Security already models authorities
     * as a collection) but leaves room to add finer-grained permissions later (e.g.
     * {@code ["ROLE_ADMIN", "camera:site-1:view"]}) without ever changing the claim's shape again.
     */
    public static final String AUTHORITIES = "authorities";

    private JwtClaimNames() {
    }
}
