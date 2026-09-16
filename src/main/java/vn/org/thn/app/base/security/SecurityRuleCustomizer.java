package vn.org.thn.app.base.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Extension point letting a business module (in a consuming service, e.g. {@code app-service}'s
 * {@code modules.user}) register its own per-endpoint authorization rules into {@code base}'s
 * default {@link SecurityAutoConfiguration#securityFilterChain}, without editing {@code base}
 * itself every time a module is added.
 * <p>
 * Before this existed, {@code securityFilterChain} hard-coded rules for {@code /public/user/**}
 * directly inside {@code base.security} - a package the project's own {@code AGENT.md} says not to
 * touch "trừ khi có yêu cầu nâng cấp framework" (unless explicitly upgrading the framework). That
 * meant adding a business module's authorization rules always required editing {@code base} anyway,
 * defeating the point of that rule (see the Medium finding, 2026-09-16 review). A module now just
 * registers one {@code @Component} implementing this interface next to its own controller instead.
 * <p>
 * Every registered customizer runs, in whatever order Spring provides the beans (undefined unless a
 * module orders itself with {@code @Order}), after {@code base}'s own fixed rules (docs, login, the
 * bundled {@code LanguageApi} - all genuinely part of {@code base} itself) and before the final
 * {@code anyRequest().authenticated()} catch-all - so a module's {@code requestMatchers(...)} calls
 * here still take effect ahead of that catch-all exactly as if they had been declared inline.
 */
@FunctionalInterface
public interface SecurityRuleCustomizer {

    /** Adds this module's own {@code requestMatchers(...)} rules onto the shared registry. */
    void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry);
}
