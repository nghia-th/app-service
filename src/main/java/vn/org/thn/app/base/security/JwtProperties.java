package vn.org.thn.app.base.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * base.security.jwt.mode                        -- STANDALONE / RESOURCE_SERVER (default: STANDALONE)
 * base.security.jwt.standalone.*                -- only used in STANDALONE mode, see {@link Standalone}
 * base.security.jwt.resource-server.*           -- only used in RESOURCE_SERVER mode, see {@link ResourceServer}
 * <p>
 * Design context (see the 2026-09-12 code review's Critical finding #2, and the discussion that
 * followed it): {@code app-service} is meant to become the shared {@code base} framework other
 * services in this platform will inherit from, and not every one of those services will
 * necessarily split authentication out into its own microservice - some may run standalone. Rather
 * than build two unrelated authentication mechanisms, {@code base} implements one claims contract
 * ({@link JwtClaimNames}) with two interchangeable ways of getting a verifiable token: sign it
 * locally ({@link JwtMode#STANDALONE}), or verify one signed by a separate auth service
 * ({@link JwtMode#RESOURCE_SERVER}). Switching between them is a config change, never a code
 * change - a service can run {@code standalone} in dev (so it works before any auth microservice
 * exists) and {@code resource-server} in staging/prod once one does.
 * <p>
 * The default is {@code STANDALONE}: a service that never sets {@code base.security.jwt.mode} at
 * all still boots and authenticates correctly on its own, the same "no external dependency
 * required to just run it" default {@code base.database.type=SQLITE} already follows.
 */
@ConfigurationProperties(prefix = "base.security.jwt")
public class JwtProperties {

    private JwtMode mode = JwtMode.STANDALONE;
    private Standalone standalone = new Standalone();
    private ResourceServer resourceServer = new ResourceServer();

    public JwtMode getMode() {
        return mode;
    }

    public void setMode(JwtMode mode) {
        this.mode = mode;
    }

    public Standalone getStandalone() {
        return standalone;
    }

    public void setStandalone(Standalone standalone) {
        this.standalone = standalone;
    }

    public ResourceServer getResourceServer() {
        return resourceServer;
    }

    public void setResourceServer(ResourceServer resourceServer) {
        this.resourceServer = resourceServer;
    }

    /**
     * Settings for {@link JwtMode#STANDALONE}: this service signs its own tokens with an RSA key
     * pair read from {@link #privateKeyPath}/{@link #publicKeyPath} (PEM files, PKCS#8 for the
     * private key and X.509 for the public key). Neither path has a default - a service running in
     * this mode must say explicitly where its keys live.
     * <p>
     * If the configured path doesn't exist yet: allowed to auto-generate a fresh key pair and
     * write it to that path exactly once, but <b>only</b> when the active Spring profile is
     * {@code dev} or {@code test} (a convenience for a freshly-cloned dev machine, not a production
     * behavior) - {@code staging}/{@code prod} must always find both files already present and
     * fail fast otherwise, since a silently-regenerated prod key would invalidate every
     * already-issued token and could mask a deployment/config mistake. The actual read-or-generate
     * logic lives in the standalone key-loading component (not this properties class).
     */
    public static class Standalone {

        private String privateKeyPath;
        private String publicKeyPath;
        private int keySize = 2048;
        private String issuer = "app-service";
        private Duration accessTokenTtl = Duration.ofMinutes(30);

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        public String getPublicKeyPath() {
            return publicKeyPath;
        }

        public void setPublicKeyPath(String publicKeyPath) {
            this.publicKeyPath = publicKeyPath;
        }

        /** RSA key size in bits, used only when a key pair is auto-generated (dev/test, missing file - see class doc). */
        public int getKeySize() {
            return keySize;
        }

        public void setKeySize(int keySize) {
            this.keySize = keySize;
        }

        /** The {@code iss} claim stamped on every token this service signs. */
        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        /** How long an access token is valid for after login. */
        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }
    }

    /**
     * Settings for {@link JwtMode#RESOURCE_SERVER}: this service holds no private key and exposes
     * no login endpoint - it only verifies tokens issued by a separate auth microservice, by
     * fetching that service's public key(s) from a JWKS endpoint and caching them (standard
     * {@code spring-boot-starter-oauth2-resource-server} behavior - no per-request network call).
     * <p>
     * Exactly one of {@link #jwkSetUri} (a fixed, already-resolved URL - simplest for a fixed
     * environment or local testing against a stub) or {@link #eurekaServiceId} (resolved through
     * service discovery at startup, appending {@link #jwkSetPath} - for the normal microservices
     * deployment where the auth service's address isn't fixed) should be set; which one is
     * preferred when both are present, and the actual Eureka lookup, belongs to the resource-server
     * wiring itself, not this properties class.
     */
    public static class ResourceServer {

        private String jwkSetUri;
        private String eurekaServiceId;
        private String jwkSetPath = "/.well-known/jwks.json";
        private String eurekaServerUrl = "http://localhost:8761/eureka";

        /** A fixed JWKS URL, bypassing Eureka lookup entirely. Takes precedence over {@link #eurekaServiceId} when both are set. */
        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        /** The auth microservice's Eureka application/service id, used to resolve its base URL before appending {@link #jwkSetPath}. */
        public String getEurekaServiceId() {
            return eurekaServiceId;
        }

        public void setEurekaServiceId(String eurekaServiceId) {
            this.eurekaServiceId = eurekaServiceId;
        }

        /** Path appended to the resolved auth service base URL to reach its JWK Set. */
        public String getJwkSetPath() {
            return jwkSetPath;
        }

        public void setJwkSetPath(String jwkSetPath) {
            this.jwkSetPath = jwkSetPath;
        }

        /**
         * Base URL of the Eureka server itself, used only to resolve {@link #eurekaServiceId}'s
         * instance address via Eureka's plain REST API (see {@link EurekaJwkSetUriResolver}) - not
         * read at all when {@link #jwkSetUri} is set. Defaults to a local single-node Eureka server's
         * default address.
         */
        public String getEurekaServerUrl() {
            return eurekaServerUrl;
        }

        public void setEurekaServerUrl(String eurekaServerUrl) {
            this.eurekaServerUrl = eurekaServerUrl;
        }
    }
}
