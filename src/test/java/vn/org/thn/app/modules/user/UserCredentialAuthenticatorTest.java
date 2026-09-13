package vn.org.thn.app.modules.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.org.thn.app.base.security.AuthenticatedPrincipal;
import vn.org.thn.app.modules.user.application.UserCredentialAuthenticator;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;
import vn.org.thn.app.modules.user.infrastructure.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers every branch of {@link UserCredentialAuthenticator#authenticate} - in particular that all
 * three failure cases (no such user, wrong password, inactive account) return {@code null}
 * uniformly rather than a distinguishable error, per {@code CredentialAuthenticator}'s
 * anti-username-enumeration contract.
 */
@ExtendWith(MockitoExtension.class)
class UserCredentialAuthenticatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCredentialAuthenticator credentialAuthenticator;

    private UserEntity activeUser() {
        UserEntity entity = new UserEntity();
        entity.setId(1L);
        entity.setUsername("john_doe");
        entity.setPassword("$2a$10$placeholderHashNotARealBcryptHash");
        entity.setStatus("ACTIVE");
        entity.setRole("ADMIN");
        return entity;
    }

    @Test
    @DisplayName("Should return a principal with a ROLE_-prefixed authority when credentials are valid")
    void authenticate_validCredentials_returnsPrincipal() {
        when(userRepository.findByUsername("john_doe")).thenReturn(activeUser());
        when(passwordEncoder.matches("S3curePass!", "$2a$10$placeholderHashNotARealBcryptHash")).thenReturn(true);

        AuthenticatedPrincipal principal = credentialAuthenticator.authenticate("john_doe", "S3curePass!");

        assertNotNull(principal);
        assertEquals(1L, principal.userId());
        assertEquals("john_doe", principal.username());
        assertEquals(java.util.List.of("ROLE_ADMIN"), principal.authorities());
    }

    @Test
    @DisplayName("Should return null when no user has the given username")
    void authenticate_unknownUsername_returnsNull() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertNull(credentialAuthenticator.authenticate("ghost", "whatever"));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Should return null when the password does not match")
    void authenticate_wrongPassword_returnsNull() {
        when(userRepository.findByUsername("john_doe")).thenReturn(activeUser());
        when(passwordEncoder.matches("wrong", "$2a$10$placeholderHashNotARealBcryptHash")).thenReturn(false);

        assertNull(credentialAuthenticator.authenticate("john_doe", "wrong"));
    }

    @Test
    @DisplayName("Should return null when the account is not ACTIVE, even with a correct password")
    void authenticate_inactiveAccount_returnsNull() {
        UserEntity inactive = activeUser();
        inactive.setStatus("INACTIVE");
        when(userRepository.findByUsername("john_doe")).thenReturn(inactive);
        when(passwordEncoder.matches("S3curePass!", "$2a$10$placeholderHashNotARealBcryptHash")).thenReturn(true);

        assertNull(credentialAuthenticator.authenticate("john_doe", "S3curePass!"));
    }

    @Test
    @DisplayName("Should return null when the stored password hash is null (legacy row, no password set yet)")
    void authenticate_noPasswordSet_returnsNull() {
        UserEntity noPassword = activeUser();
        noPassword.setPassword(null);
        when(userRepository.findByUsername("john_doe")).thenReturn(noPassword);

        assertNull(credentialAuthenticator.authenticate("john_doe", "anything"));
        verifyNoInteractions(passwordEncoder);
    }
}
