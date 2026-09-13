package vn.org.thn.app.modules.user.infrastructure;

import org.springframework.stereotype.Repository;
import vn.org.thn.app.base.persistence.repository.BaseRepositoryImpl;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;

@Repository
public class UserRepository extends BaseRepositoryImpl<UserEntity, Long> {

    public boolean existsByUsername(String username) {
        return query().eq(UserEntity::getUsername, username).exists();
    }

    public boolean existsByEmail(String email) {
        return query().eq(UserEntity::getEmail, email).exists();
    }

    /** Looked up by {@code UserCredentialAuthenticator} during standalone-mode login. Null if no user has this username. */
    public UserEntity findByUsername(String username) {
        return query().eq(UserEntity::getUsername, username).one();
    }
}
