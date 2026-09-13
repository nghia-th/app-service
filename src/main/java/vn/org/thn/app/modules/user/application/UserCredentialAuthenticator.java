package vn.org.thn.app.modules.user.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.security.AuthenticatedPrincipal;
import vn.org.thn.app.base.security.CredentialAuthenticator;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;
import vn.org.thn.app.modules.user.infrastructure.UserRepository;

import java.util.List;

/**
 * {@code app-service}'s {@link CredentialAuthenticator}: checks a username/password pair against
 * {@link UserEntity} (via {@link UserRepository}) and the shared {@link PasswordEncoder} bean.
 * Registered as a plain {@code @Component} so it's the one bean satisfying {@link
 * CredentialAuthenticator} that {@code base.security}'s standalone-mode login endpoint depends on -
 * see that interface's javadoc for why {@code base} can't provide this itself.
 * <p>
 * {@link UserEntity#getRole()} stores a bare role name (e.g. {@code "USER"}, {@code "ADMIN"}, see
 * {@code UserService#createUser}'s default) - this class is the one place that turns it into the
 * Spring Security authority string ({@code "ROLE_" + role}) carried in the token's {@code
 * authorities} claim (see {@code JwtClaimNames#AUTHORITIES}).
 */
@Component
public class UserCredentialAuthenticator implements CredentialAuthenticator {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public AuthenticatedPrincipal authenticate(String username, String rawPassword) {
        UserEntity entity = userRepository.findByUsername(username);
        if (entity == null) {
            return null;
        }
        if (entity.getPassword() == null || !passwordEncoder.matches(rawPassword, entity.getPassword())) {
            return null;
        }
        if (!"ACTIVE".equals(entity.getStatus())) {
            return null;
        }
        String role = entity.getRole() != null ? entity.getRole() : "USER";
        return new AuthenticatedPrincipal(entity.getId(), entity.getUsername(), List.of("ROLE_" + role));
    }
}
