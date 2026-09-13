package vn.org.thn.app.base.security.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.app.base.core.exception.BusinessException;
import vn.org.thn.app.base.core.exception.CommonErrorCode;
import vn.org.thn.app.base.core.response.ApiResponse;
import vn.org.thn.app.base.security.AuthenticatedPrincipal;
import vn.org.thn.app.base.security.CredentialAuthenticator;
import vn.org.thn.app.base.security.StandaloneTokenService;
import vn.org.thn.app.base.security.api.dto.LoginRequest;
import vn.org.thn.app.base.security.api.dto.LoginResponse;
import vn.org.thn.app.base.web.controller.BaseCtl;

/**
 * Login endpoint for {@code base.security.jwt.mode=STANDALONE} only - a service running
 * {@code RESOURCE_SERVER} mode never signs tokens itself (a separate auth microservice does), so
 * this controller doesn't even exist in that mode ({@code @ConditionalOnProperty} keeps its bean,
 * and therefore its {@code /public/auth/login} route, out of the context entirely rather than
 * exposing an endpoint that would always fail).
 * <p>
 * Under {@code /public/...} because it must be reachable by a caller with no token yet - this is
 * the one path the eventual real {@link org.springframework.security.web.SecurityFilterChain}
 * (see the design discussion following the 2026-09-12 review's Critical finding #2) must leave
 * open to anonymous callers, alongside health checks and API docs.
 */
@Tag(name = "Auth API", description = "Standalone-mode login (base.security.jwt.mode=STANDALONE only)")
@RestController
@RequestMapping("/public/auth")
@ConditionalOnProperty(prefix = "base.security.jwt", name = "mode", havingValue = "STANDALONE", matchIfMissing = true)
public class AuthCtl extends BaseCtl {

    @Autowired
    private CredentialAuthenticator credentialAuthenticator;

    @Autowired
    private StandaloneTokenService standaloneTokenService;

    @Operation(summary = "Login", description = "Exchange a username/password pair for a signed JWT access token")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedPrincipal principal = credentialAuthenticator.authenticate(request.getUsername(), request.getPassword());
        if (principal == null) {
            // Deliberately the same error for "no such user", "wrong password" and "inactive
            // account" - see CredentialAuthenticator#authenticate's javadoc on username enumeration.
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Invalid username or password");
        }
        String token = standaloneTokenService.issueAccessToken(principal);
        return ok(new LoginResponse(token, standaloneTokenService.getAccessTokenTtlSeconds()));
    }
}
