package vn.org.thn.app.base.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * base.web.cors.allowed-origin-patterns  -- default ["*"] (dev-friendly; tighten per environment)
 * base.web.cors.allowed-methods          -- default [GET, POST, PUT, DELETE, OPTIONS]
 * base.web.cors.allowed-headers          -- default ["*"]
 * base.web.cors.exposed-headers          -- default [X-Request-Id, token]
 * <p>
 * Backs {@link BaseWebAutoConfiguration#corsConfigurer}. Previously this policy was hardcoded with
 * no way to tighten it for staging/prod short of overriding the whole {@code WebMvcConfigurer} bean
 * - see the CORS Low finding (2026-09-12 review). The default here is unchanged (still fully
 * permissive, since there is no auth layer yet - see Critical finding #2), but a consuming service
 * can now narrow {@code allowed-origin-patterns} (and the rest) per environment via
 * application-*.yaml, with no code change or bean override needed.
 */
@ConfigurationProperties(prefix = "base.web.cors")
public class CorsProperties {

    private List<String> allowedOriginPatterns = List.of("*");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private List<String> exposedHeaders = List.of("X-Request-Id", "token");

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }
}
