package vn.org.thn.app.base.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers {@link EurekaJwkSetUriResolver}'s JWKS URL resolution: a fixed {@code jwkSetUri} always
 * wins (no network call needed), and the Eureka JSON parsing handles both shapes Eureka's
 * XML-derived JSON can return for the {@code instance} field (a single object for one instance, an
 * array for several) - see {@link #parseFirstUpInstanceBaseUrl} for the network-free parsing logic
 * under test.
 */
class EurekaJwkSetUriResolverTest {

    @Test
    @DisplayName("Should return the fixed jwkSetUri as-is when set, without needing eureka-service-id")
    void resolveJwkSetUri_fixedUriConfigured_returnsItDirectly() {
        JwtProperties.ResourceServer config = new JwtProperties.ResourceServer();
        config.setJwkSetUri("https://auth.example.com/.well-known/jwks.json");

        String resolved = new EurekaJwkSetUriResolver(config).resolveJwkSetUri();

        assertEquals("https://auth.example.com/.well-known/jwks.json", resolved);
    }

    @Test
    @DisplayName("Should throw when neither jwkSetUri nor eureka-service-id is configured")
    void resolveJwkSetUri_neitherConfigured_throws() {
        JwtProperties.ResourceServer config = new JwtProperties.ResourceServer();

        assertThrows(IllegalStateException.class, () -> new EurekaJwkSetUriResolver(config).resolveJwkSetUri());
    }

    @Test
    @DisplayName("Should pick the UP instance's homePageUrl from a single-instance (object-shaped) Eureka response")
    void parseFirstUpInstanceBaseUrl_singleInstanceObject_returnsHomePageUrl() {
        String json = """
                {
                  "application": {
                    "name": "AUTH-SERVICE",
                    "instance": {
                      "status": "UP",
                      "homePageUrl": "http://10.0.0.5:8080/",
                      "ipAddr": "10.0.0.5",
                      "port": {"$": 8080, "@enabled": "true"}
                    }
                  }
                }
                """;

        String baseUrl = EurekaJwkSetUriResolver.parseFirstUpInstanceBaseUrl(json, "AUTH-SERVICE");

        assertEquals("http://10.0.0.5:8080", baseUrl);
    }

    @Test
    @DisplayName("Should pick the first UP instance from a multi-instance (array-shaped) Eureka response")
    void parseFirstUpInstanceBaseUrl_multipleInstancesArray_skipsDownAndPicksUp() {
        String json = """
                {
                  "application": {
                    "name": "AUTH-SERVICE",
                    "instance": [
                      {"status": "DOWN", "homePageUrl": "http://10.0.0.4:8080/"},
                      {"status": "UP", "homePageUrl": "http://10.0.0.6:8080/"}
                    ]
                  }
                }
                """;

        String baseUrl = EurekaJwkSetUriResolver.parseFirstUpInstanceBaseUrl(json, "AUTH-SERVICE");

        assertEquals("http://10.0.0.6:8080", baseUrl);
    }

    @Test
    @DisplayName("Should fall back to ipAddr+port when homePageUrl is absent")
    void parseFirstUpInstanceBaseUrl_noHomePageUrl_fallsBackToIpAndPort() {
        String json = """
                {
                  "application": {
                    "name": "AUTH-SERVICE",
                    "instance": {
                      "status": "UP",
                      "ipAddr": "10.0.0.7",
                      "port": {"$": 9090, "@enabled": "true"}
                    }
                  }
                }
                """;

        String baseUrl = EurekaJwkSetUriResolver.parseFirstUpInstanceBaseUrl(json, "AUTH-SERVICE");

        assertEquals("http://10.0.0.7:9090", baseUrl);
    }

    @Test
    @DisplayName("Should throw when no instance is UP")
    void parseFirstUpInstanceBaseUrl_noUpInstance_throws() {
        String json = """
                {
                  "application": {
                    "name": "AUTH-SERVICE",
                    "instance": {"status": "DOWN", "homePageUrl": "http://10.0.0.4:8080/"}
                  }
                }
                """;

        assertThrows(IllegalStateException.class,
                () -> EurekaJwkSetUriResolver.parseFirstUpInstanceBaseUrl(json, "AUTH-SERVICE"));
    }
}
