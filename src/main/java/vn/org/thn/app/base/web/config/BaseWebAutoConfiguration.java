package vn.org.thn.app.base.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.org.thn.app.base.web.exception.GlobalExceptionHandler;
import vn.org.thn.app.base.web.filter.RequestContextFilter;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Auto-registers shared web components for any service that depends on this module - no
 * @ComponentScan wiring needed on the consuming service's side. Listed in
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
 * <p>
 * Each bean is {@code @ConditionalOnMissingBean}: a consuming service that needs different
 * behavior (a stricter CORS policy, no request-logging filter, its own OpenAPI bean) just
 * declares its own bean of that type and this default is skipped.
 */
@AutoConfiguration(after = WebMvcAutoConfiguration.class)
public class BaseWebAutoConfiguration {

    /** Registers the default {@link GlobalExceptionHandler}, unless the consuming service defines its own. */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /** Registers {@link RequestContextFilter} against every URL, ahead of most other filters (order 1). */
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration() {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>(new RequestContextFilter());
        registration.setOrder(1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /** Permissive default CORS policy (any origin, common methods, any header) - tighten by declaring a service-specific {@link WebMvcConfigurer} bean instead. */
    @Bean
    @ConditionalOnMissingBean(WebMvcConfigurer.class)
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Request-Id", "token");
            }
        };
    }

    /** Default OpenAPI definition: a token-header security scheme plus one server entry for localhost and one per LAN-reachable IPv4 address, so Swagger UI works when accessed from another machine on the network. Only registered when springdoc/swagger is on the classpath. */
    @Bean
    @ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI customOpenAPI(@Value("${server.port:8080}") String port) {
        List<Server> servers = new ArrayList<>();

        Server local = new Server();
        local.setUrl("http://localhost:" + port);
        servers.add(local);

        for (String ip : lanIpAddresses()) {
            Server server = new Server();
            server.setUrl("http://" + ip + ":" + port);
            servers.add(server);
        }

        return new OpenAPI()
                .servers(servers)
                .addSecurityItem(new SecurityRequirement().addList("token"))
                .components(new Components()
                        .addSecuritySchemes("token", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .name("token")
                                .in(SecurityScheme.In.HEADER)));
    }

    /** LAN-reachable IPv4 addresses of this host, skipping loopback/down interfaces and common virtual adapters. */
    private static Set<String> lanIpAddresses() {
        Set<String> addresses = new LinkedHashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback()) {
                    continue;
                }
                String name = network.getDisplayName().toLowerCase();
                if (name.contains("virtual") || name.contains("vmware") || name.contains("docker") || name.contains("hyper-v")) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = network.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && address.isSiteLocalAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
            return Collections.emptySet();
        }
        return addresses;
    }
}
