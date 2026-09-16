package vn.org.thn.app.modules.user.api;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.security.SecurityRuleCustomizer;

/**
 * {@link UserCtl}'s own authorization rules, registered into {@code base}'s shared
 * {@code SecurityAutoConfiguration#securityFilterChain} via {@link SecurityRuleCustomizer} - see
 * that interface's javadoc for why this lives here, next to the controller it protects, instead of
 * hard-coded inside {@code base.security} (AGENT.md's rule against editing {@code base} for a
 * business module's own concern).
 * <p>
 * Reads ({@code page}/{@code getById}) need {@code USER} or {@code ADMIN}; writes
 * (create/update/delete) are {@code ADMIN}-only - this module is a demo of the pattern, not a fixed
 * rule about who may manage users.
 */
@Component
public class UserSecurityRules implements SecurityRuleCustomizer {

    @Override
    public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers(HttpMethod.GET, "/public/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/public/user").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/public/user/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/public/user/**").hasRole("ADMIN");
    }
}
