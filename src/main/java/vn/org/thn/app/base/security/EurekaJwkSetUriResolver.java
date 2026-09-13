package vn.org.thn.app.base.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the final JWKS URL {@link JwtMode#RESOURCE_SERVER} verifies tokens against.
 * <p>
 * If {@link JwtProperties.ResourceServer#getJwkSetUri()} is set, it is used as-is (a fixed URL,
 * simplest for a stub/local test setup - see that field's javadoc). Otherwise, the auth
 * microservice's address is looked up once, at startup, straight from a Eureka server's plain REST
 * API ({@code GET {eureka-server-url}/apps/{eureka-service-id}}, {@code Accept: application/json}) -
 * deliberately not the full {@code spring-cloud-starter-netflix-eureka-client} dependency: at the
 * time this was written, this project runs a pre-release Spring Boot version (see build.gradle)
 * that no published Spring Cloud release train yet targets, and pulling in an incompatible/snapshot
 * Spring Cloud BOM here would risk breaking the whole build with no way to verify it compiles.
 * <p>
 * This is a deliberately narrow, dependency-free substitute: it covers "find the auth service's
 * base URL once at startup", not full Eureka client behavior (client-side load balancing across
 * instances, periodic re-registration/re-resolution, health propagation). If the auth service's
 * registered address changes later (redeployed on a new host/port), app-service must be restarted
 * to pick it up. Swap this out for the real Spring Cloud Eureka client once a compatible release
 * exists, if that resilience is needed.
 */
final class EurekaJwkSetUriResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JwtProperties.ResourceServer config;
    private final HttpClient httpClient;

    EurekaJwkSetUriResolver(JwtProperties.ResourceServer config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** The final JWKS URL to configure {@code NimbusJwtDecoder.withJwkSetUri(...)} with. */
    String resolveJwkSetUri() {
        if (config.getJwkSetUri() != null && !config.getJwkSetUri().isBlank()) {
            return config.getJwkSetUri();
        }
        String serviceId = config.getEurekaServiceId();
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalStateException(
                    "base.security.jwt.mode=RESOURCE_SERVER requires either "
                            + "base.security.jwt.resource-server.jwk-set-uri or "
                            + "base.security.jwt.resource-server.eureka-service-id to be set");
        }
        String baseUrl = fetchInstanceBaseUrl(serviceId);
        String path = config.getJwkSetPath();
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private String fetchInstanceBaseUrl(String serviceId) {
        String eurekaAppsUrl = trimTrailingSlash(config.getEurekaServerUrl()) + "/apps/" + serviceId;
        HttpRequest request = HttpRequest.newBuilder(URI.create(eurekaAppsUrl))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Eureka lookup for service '" + serviceId + "' at " + eurekaAppsUrl
                                + " returned HTTP " + response.statusCode());
            }
            return parseFirstUpInstanceBaseUrl(response.body(), serviceId);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                    "Could not reach Eureka server at " + config.getEurekaServerUrl()
                            + " to resolve service '" + serviceId + "'", e);
        }
    }

    /**
     * Pure parsing of Eureka's {@code GET /apps/{serviceId}} JSON response, split out from the
     * network call above so it can be unit tested without a running Eureka server. Picks the first
     * instance with {@code status=UP}, preferring its {@code homePageUrl} and falling back to
     * {@code ipAddr}+{@code port} if that's absent.
     */
    static String parseFirstUpInstanceBaseUrl(String eurekaAppsJson, String serviceId) {
        try {
            JsonNode root = MAPPER.readTree(eurekaAppsJson);
            JsonNode instanceNode = root.path("application").path("instance");
            // Eureka's XML-derived JSON collapses a single instance down to a plain object instead
            // of a one-element array - normalize both shapes into a list of instance nodes.
            List<JsonNode> instances = new ArrayList<>();
            if (instanceNode.isArray()) {
                instanceNode.forEach(instances::add);
            } else {
                instances.add(instanceNode);
            }
            for (JsonNode instance : instances) {
                if (instance == null || instance.isMissingNode()) {
                    continue;
                }
                String status = instance.path("status").asText("");
                if (!"UP".equals(status)) {
                    continue;
                }
                String homePageUrl = instance.path("homePageUrl").asText(null);
                if (homePageUrl != null && !homePageUrl.isBlank()) {
                    return trimTrailingSlash(homePageUrl);
                }
                String ipAddr = instance.path("ipAddr").asText(null);
                int port = instance.path("port").path("$").asInt(-1);
                if (ipAddr != null && port > 0) {
                    return "http://" + ipAddr + ":" + port;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not parse Eureka response for service '" + serviceId + "'", e);
        }
        throw new IllegalStateException("No UP instance of service '" + serviceId + "' registered in Eureka");
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
