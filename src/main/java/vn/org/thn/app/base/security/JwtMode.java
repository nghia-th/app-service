package vn.org.thn.app.base.security;

/**
 * Which of the two JWT operating modes {@code base} runs in, selected via {@code base.security.jwt.mode}.
 * <p>
 * Both modes share the exact same claims contract (see {@link JwtClaimNames}) - a service never
 * needs to know which mode issued a token it is verifying, and {@code @PreAuthorize} checks written
 * against the resulting {@code Authentication} work identically either way. Only how the signing
 * key is obtained differs.
 */
public enum JwtMode {

    /**
     * This service is its own authorization server: it exposes a login endpoint, checks
     * username/password against the local {@code UserEntity} table, and signs tokens itself with
     * an RSA key pair read from disk (see {@link JwtProperties.Standalone}). Use this when no
     * separate auth microservice exists yet, or for a deployment that intentionally doesn't split
     * authentication into its own service.
     */
    STANDALONE,

    /**
     * This service only verifies tokens issued elsewhere: no login endpoint is exposed, and no
     * private key is ever held here. The verification (public) key is resolved from a JWKS
     * endpoint published by a separate auth microservice, discovered via Eureka (see
     * {@link JwtProperties.ResourceServer}). Use this once a dedicated auth/identity service is
     * running in the same service-discovery registry.
     */
    RESOURCE_SERVER
}
