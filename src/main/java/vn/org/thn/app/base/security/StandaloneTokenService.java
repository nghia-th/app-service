package vn.org.thn.app.base.security;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;

/**
 * Signs access tokens for {@link JwtMode#STANDALONE}, following the shared claims contract (see
 * {@link JwtClaimNames}). The counterpart {@code JwtDecoder} bean (also registered only in
 * standalone mode) verifies exactly what this class produces, using the same RSA key pair's public
 * half.
 */
public final class StandaloneTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties.Standalone config;

    public StandaloneTokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.config = properties.getStandalone();
    }

    /** Mints a signed, time-limited access token for an already-authenticated principal. */
    public String issueAccessToken(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(config.getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(config.getAccessTokenTtl()))
                .subject(principal.username())
                .claim(JwtClaimNames.USER_ID, principal.userId())
                .claim(JwtClaimNames.AUTHORITIES, principal.authorities())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** How many seconds an access token minted right now would be valid for - for a login response's {@code expiresIn}. */
    public long getAccessTokenTtlSeconds() {
        return config.getAccessTokenTtl().toSeconds();
    }
}
