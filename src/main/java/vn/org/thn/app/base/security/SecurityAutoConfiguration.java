package vn.org.thn.app.base.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auto-registers JWT authentication/authorization for any service depending on {@code base} -
 * listed in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * same mechanism as {@link vn.org.thn.app.base.web.config.BaseWebAutoConfiguration}.
 * <p>
 * {@link #passwordEncoder()} is registered unconditionally (password hashing is a service concern
 * independent of {@link JwtMode}). The RSA key provider, {@link JwtEncoder} and
 * {@link StandaloneTokenService} beans here are specific to {@link JwtMode#STANDALONE} - that mode
 * signs its own tokens locally, so it needs no counterpart in {@link JwtMode#RESOURCE_SERVER}, which
 * never signs anything. Both modes register a {@link JwtDecoder}, but never at the same time: the
 * two {@code jwtDecoder} bean methods below are mutually exclusive on the same
 * {@code base.security.jwt.mode} property - {@link JwtMode#STANDALONE} verifies with the local key
 * pair's public half, {@link JwtMode#RESOURCE_SERVER} verifies via a remote JWKS (see
 * {@link EurekaJwkSetUriResolver}).
 * <p>
 * {@link #securityFilterChain} carries the real per-endpoint authorization rules - see its own
 * javadoc - built following the design discussion after the 2026-09-12 review's Critical
 * finding #2.
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity
public class SecurityAutoConfiguration {

    /** BCrypt password hashing, used by any consuming service's own user-creation flow (e.g. app-service's {@code UserService#createUser}) and by standalone-mode credential verification alike. */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(prefix = "base.security.jwt", name = "mode", havingValue = "STANDALONE", matchIfMissing = true)
    StandaloneRsaKeyProvider standaloneRsaKeyProvider(JwtProperties properties, Environment environment) {
        return new StandaloneRsaKeyProvider(properties, environment);
    }

    /** Signs tokens with the private half of {@link StandaloneRsaKeyProvider}'s key pair. A single fixed key id is enough - standalone mode never rotates keys or serves more than one at once. */
    @Bean
    @ConditionalOnProperty(prefix = "base.security.jwt", name = "mode", havingValue = "STANDALONE", matchIfMissing = true)
    public JwtEncoder jwtEncoder(StandaloneRsaKeyProvider keyProvider) {
        RSAKey rsaKey = new RSAKey.Builder(keyProvider.getPublicKey())
                .privateKey(keyProvider.getPrivateKey())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID("standalone")
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    /** Verifies tokens with the public half of the same key pair {@link #jwtEncoder} signs with - no JWKS round trip needed, this service already holds the key. */
    @Bean
    @ConditionalOnProperty(prefix = "base.security.jwt", name = "mode", havingValue = "STANDALONE", matchIfMissing = true)
    public JwtDecoder jwtDecoder(StandaloneRsaKeyProvider keyProvider) {
        return NimbusJwtDecoder.withPublicKey(keyProvider.getPublicKey()).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "base.security.jwt", name = "mode", havingValue = "STANDALONE", matchIfMissing = true)
    public StandaloneTokenService standaloneTokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        return new StandaloneTokenService(jwtEncoder, properties);
    }

    /**
     * Verifies tokens signed by a separate auth microservice, via its published JWKS - see
     * {@link EurekaJwkSetUriResolver} for how that service's JWKS URL is resolved (fixed
     * {@code jwk-set-uri}, or a one-time Eureka lookup by {@code eureka-service-id}).
     * {@link NimbusJwtDecoder#withJwkSetUri} handles fetching/caching/rotating the actual signing
     * keys from there - no further round trip per request.
     */
    @Bean
    @ConditionalOnProperty(prefix = "base.security.jwt", name = "mode", havingValue = "RESOURCE_SERVER")
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        String jwkSetUri = new EurekaJwkSetUriResolver(properties.getResourceServer()).resolveJwkSetUri();
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * Maps the {@link JwtClaimNames#AUTHORITIES} claim (a JSON array of Spring Security authority
     * strings, e.g. {@code ["ROLE_ADMIN"]} - see that claim's javadoc) straight onto
     * {@link org.springframework.security.core.GrantedAuthority}, with no extra prefix - the claim
     * already carries the {@code ROLE_} prefix {@code hasRole(...)}/{@code hasAnyRole(...)} expect,
     * so adding Spring's default {@code SCOPE_} prefix here would break every role check.
     * {@code @ConditionalOnMissingBean} so a consuming service can plug in a different
     * claims-to-authorities mapping without overriding the whole {@link #securityFilterChain}.
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(JwtClaimNames.AUTHORITIES);
        authoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /**
     * Real per-endpoint authorization rules, replacing the temporary permit-everything placeholder
     * from when {@code spring-boot-starter-security} first landed on the classpath (see the design
     * discussion following the 2026-09-12 review's Critical finding #2). Rules, in order:
     * <ul>
     * <li>API documentation ({@code /api.html}, {@code /doc}, Swagger UI/webjars/OpenAPI JSON) -
     * always open, no sensitive data.</li>
     * <li>{@code POST /public/auth/login} - must be reachable with no token yet (see {@link
     * vn.org.thn.app.base.security.api.AuthCtl}).</li>
     * <li>{@code LanguageApi}: only the three read endpoints ({@code GET .../list}, {@code GET
     * .../{lang}}, bare {@code GET} for all languages) are public; every write is
     * {@code ADMIN}-only.</li>
     * <li>{@code UserCtl}: reads ({@code page}/{@code getById}) need {@code USER} or {@code ADMIN};
     * writes (create/update/delete) are {@code ADMIN}-only - this module is a demo of the pattern,
     * not a fixed rule about who may manage users.</li>
     * <li>Everything else requires a valid, signed token (no anonymous access by default for any
     * future endpoint that forgets to declare its own rule).</li>
     * </ul>
     * {@code @ConditionalOnMissingBean} so a consuming service (or a later piece of {@code base}
     * itself) can supply an entirely different {@link SecurityFilterChain} instead.
     * <p>
     * CSRF protection is disabled and the session policy is {@link SessionCreationPolicy#STATELESS}
     * because this API is token-based (a JWT presented in the {@code Authorization} header on every
     * request, no server-side session cookie) - CSRF defenses exist specifically for
     * cookie-authenticated browser sessions and are meaningless (and only obstructive to
     * non-browser clients) for this authentication model.
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api.html", "/api.html/**", "/doc", "/doc/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/public/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/public/language", "/public/language/list", "/public/language/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/public/language").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/public/language").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/public/language", "/public/language/deletes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/public/language/export").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/public/user/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/public/user").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/public/user/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/public/user/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
