package vn.org.thn.app.base.security;

import java.util.List;

/**
 * The result of a successful {@link CredentialAuthenticator#authenticate} call: everything
 * {@link StandaloneTokenService} needs to mint a token matching the shared claims contract (see
 * {@link JwtClaimNames}).
 *
 * @param userId      the authenticated user's id - becomes the {@link JwtClaimNames#USER_ID} claim.
 * @param username    the authenticated user's username - becomes the standard {@code sub} claim.
 * @param authorities Spring Security authority strings for this user (e.g. {@code ["ROLE_ADMIN"]}) -
 *                    becomes the {@link JwtClaimNames#AUTHORITIES} claim as-is, with no role
 *                    hierarchy applied (see {@link JwtClaimNames#AUTHORITIES}'s javadoc for why).
 */
public record AuthenticatedPrincipal(Long userId, String username, List<String> authorities) {
}
