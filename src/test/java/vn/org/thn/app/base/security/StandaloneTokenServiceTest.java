package vn.org.thn.app.base.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies {@link StandaloneTokenService} issues tokens matching the shared claims contract (see
 * {@link JwtClaimNames}), and that a {@link JwtDecoder} built from the same key pair's public half
 * (exactly how {@code SecurityAutoConfiguration#jwtDecoder} builds it) can verify what this class
 * signs.
 */
class StandaloneTokenServiceTest {

    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;
    private JwtProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID("standalone")
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        jwtEncoder = new NimbusJwtEncoder(jwkSource);
        jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        properties = new JwtProperties();
        properties.getStandalone().setIssuer("app-service");
        properties.getStandalone().setAccessTokenTtl(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("Should issue a token carrying userId/authorities/subject/issuer per the shared claims contract")
    void issueAccessToken_validPrincipal_producesVerifiableTokenWithExpectedClaims() {
        StandaloneTokenService tokenService = new StandaloneTokenService(jwtEncoder, properties);
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(42L, "john_doe", List.of("ROLE_ADMIN"));

        Instant now = Instant.now();
        String token = tokenService.issueAccessToken(principal);

        Jwt decoded = jwtDecoder.decode(token);

        assertEquals("john_doe", decoded.getSubject());
        assertEquals("app-service", decoded.getClaimAsString("iss"));
        assertEquals(42, ((Number) decoded.getClaim(JwtClaimNames.USER_ID)).longValue());
        assertEquals(List.of("ROLE_ADMIN"), decoded.getClaim(JwtClaimNames.AUTHORITIES));
        // Wide (5s) tolerance around "now" absorbs JWT numeric-date second-truncation and test
        // execution time without being a flaky assertion - the exact TTL is verified separately below.
        assertTrue(decoded.getIssuedAt().isAfter(now.minusSeconds(5)), "issuedAt should be close to now");
        assertTrue(decoded.getIssuedAt().isBefore(now.plusSeconds(5)), "issuedAt should not be in the future");
        assertTrue(decoded.getExpiresAt().isAfter(decoded.getIssuedAt()));
        assertEquals(Duration.ofMinutes(30).toSeconds(),
                Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt()).toSeconds(),
                "expiresAt - issuedAt must equal the configured TTL exactly (both timestamps are floored to the same second, so the integer-second TTL survives truncation)");
    }

    @Test
    @DisplayName("getAccessTokenTtlSeconds should reflect the configured TTL")
    void getAccessTokenTtlSeconds_returnsConfiguredDurationInSeconds() {
        StandaloneTokenService tokenService = new StandaloneTokenService(jwtEncoder, properties);

        assertEquals(1800L, tokenService.getAccessTokenTtlSeconds());
    }

    @Test
    @DisplayName("A token issued for a USER-role principal should not carry ADMIN authority (no implicit role hierarchy)")
    void issueAccessToken_userRolePrincipal_carriesOnlyItsOwnAuthority() {
        StandaloneTokenService tokenService = new StandaloneTokenService(jwtEncoder, properties);
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(1L, "plain_user", List.of("ROLE_USER"));

        String token = tokenService.issueAccessToken(principal);
        Jwt decoded = jwtDecoder.decode(token);

        assertEquals(List.of("ROLE_USER"), decoded.getClaim(JwtClaimNames.AUTHORITIES));
    }
}
