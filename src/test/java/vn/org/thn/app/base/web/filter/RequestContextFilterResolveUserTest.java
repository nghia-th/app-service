package vn.org.thn.app.base.web.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the 2026-09-12 review's Critical finding #2: {@link RequestContextFilter#resolveUser}
 * used to trust client-supplied headers ({@code X-User-Id}, {@code X-Username}, {@code username},
 * {@code user}) with no verification - any caller could claim to be anyone. It now reads the
 * authenticated principal straight from {@link SecurityContextHolder} instead, which by the time
 * this filter runs (order 1) has already been populated - or not - by Spring Security's OAuth2
 * resource server filter earlier in the chain.
 */
class RequestContextFilterResolveUserTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return the authenticated principal's name when a real authentication is present")
    void resolveUser_authenticatedPrincipal_returnsItsName() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "john_doe", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals("john_doe", RequestContextFilter.resolveUser(null));
    }

    @Test
    @DisplayName("Should return null for an anonymous (unauthenticated) request")
    void resolveUser_anonymousAuthentication_returnsNull() {
        var anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertNull(RequestContextFilter.resolveUser(null));
    }

    @Test
    @DisplayName("Should return null when no authentication is present at all")
    void resolveUser_noAuthentication_returnsNull() {
        assertNull(RequestContextFilter.resolveUser(null));
    }

    @Test
    @DisplayName("Should return null for an authentication object marked as not authenticated")
    void resolveUser_notAuthenticatedToken_returnsNull() {
        var authentication = new UsernamePasswordAuthenticationToken("john_doe", "password");
        assertFalse(authentication.isAuthenticated(), "the 2-arg constructor must default to unauthenticated");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertNull(RequestContextFilter.resolveUser(null));
    }

    @Test
    @DisplayName("Client-supplied identity headers must never be trusted, even if present")
    void resolveUser_neverReadsHeadersEvenIfWereSpoofed() {
        // No mock HttpServletRequest is set up at all - resolveUser must not attempt to read any
        // header off it. An authenticated SecurityContext is the only source of truth now.
        var authentication = new UsernamePasswordAuthenticationToken(
                "real_user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals("real_user", RequestContextFilter.resolveUser(null));
    }
}
