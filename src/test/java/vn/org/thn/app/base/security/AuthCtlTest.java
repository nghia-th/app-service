package vn.org.thn.app.base.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.org.thn.app.base.core.exception.BusinessException;
import vn.org.thn.app.base.core.exception.CommonErrorCode;
import vn.org.thn.app.base.core.response.ApiResponse;
import vn.org.thn.app.base.security.api.AuthCtl;
import vn.org.thn.app.base.security.api.dto.LoginRequest;
import vn.org.thn.app.base.security.api.dto.LoginResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers {@link AuthCtl#login}: a valid credential pair returns a token wrapped in the standard
 * {@link ApiResponse} envelope, and an invalid one throws a {@link BusinessException} carrying
 * {@link CommonErrorCode#UNAUTHORIZED} - the same error regardless of *why* it failed (no such
 * user, wrong password, inactive account - see {@link CredentialAuthenticator}'s anti-username-
 * enumeration contract), never something the client could use to tell those cases apart.
 */
@ExtendWith(MockitoExtension.class)
class AuthCtlTest {

    @Mock
    private CredentialAuthenticator credentialAuthenticator;

    @Mock
    private StandaloneTokenService standaloneTokenService;

    @InjectMocks
    private AuthCtl authCtl;

    @Test
    @DisplayName("Should return an access token wrapped in ApiResponse when credentials are valid")
    void login_validCredentials_returnsAccessToken() {
        LoginRequest request = new LoginRequest("john_doe", "S3curePass!");
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(1L, "john_doe", java.util.List.of("ROLE_ADMIN"));

        when(credentialAuthenticator.authenticate("john_doe", "S3curePass!")).thenReturn(principal);
        when(standaloneTokenService.issueAccessToken(principal)).thenReturn("signed.jwt.token");
        when(standaloneTokenService.getAccessTokenTtlSeconds()).thenReturn(1800L);

        ResponseEntity<ApiResponse<LoginResponse>> response = authCtl.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        LoginResponse body = response.getBody().getData();
        assertEquals("signed.jwt.token", body.getAccessToken());
        assertEquals("Bearer", body.getTokenType());
        assertEquals(1800L, body.getExpiresIn());
    }

    @Test
    @DisplayName("Should throw BusinessException(UNAUTHORIZED) when the credential authenticator returns null")
    void login_invalidCredentials_throwsUnauthorized() {
        LoginRequest request = new LoginRequest("john_doe", "wrong");
        when(credentialAuthenticator.authenticate("john_doe", "wrong")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> authCtl.login(request));

        assertEquals(CommonErrorCode.UNAUTHORIZED, ex.getErrorCode());
        verifyNoInteractions(standaloneTokenService);
    }
}
