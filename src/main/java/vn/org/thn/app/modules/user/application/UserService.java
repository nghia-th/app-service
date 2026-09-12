package vn.org.thn.app.modules.user.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.app.base.core.dto.page.PageResponse;
import vn.org.thn.app.base.core.exception.BusinessException;
import vn.org.thn.app.base.core.exception.CommonErrorCode;
import vn.org.thn.app.modules.user.api.dto.UserCreateRequest;
import vn.org.thn.app.modules.user.api.dto.UserResponse;
import vn.org.thn.app.modules.user.api.dto.UserUpdateRequest;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;
import vn.org.thn.app.modules.user.infrastructure.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public PageResponse<UserResponse> getUsersPaged(int page, int size, String keyword, String status) {
        var query = userRepository.query();

        if (status != null && !status.isBlank()) {
            query.eq(UserEntity::getStatus, status);
        }

        if (keyword != null && !keyword.isBlank()) {
            query.and(sub -> sub
                    .like(UserEntity::getUsername, keyword)
                    .orEq(UserEntity::getFullName, keyword)
                    .orEq(UserEntity::getEmail, keyword)
            );
        }

        PageResponse<UserEntity> entityPage = query
                .orderByDesc(UserEntity::getId)
                .page(page, size)
                .pageResult();

        List<UserResponse> responses = entityPage.getContent().stream()
                .map(UserResponse::fromEntity)
                .toList();

        return PageResponse.of(responses, entityPage.getPage(), entityPage.getSize(), entityPage.getTotalElements());
    }

    public UserResponse getUserById(Long id) {
        UserEntity entity = userRepository.findById(id);
        if (entity == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "User not found with id: " + id);
        }
        return UserResponse.fromEntity(entity);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED, "Username must not be blank");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED, "Username already exists: " + request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(CommonErrorCode.VALIDATION_FAILED, "Email already exists: " + request.getEmail());
            }
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(request.getUsername());
        entity.setEmail(request.getEmail());
        entity.setFullName(request.getFullName());
        entity.setRole(request.getRole() != null ? request.getRole() : "USER");
        entity.setStatus("ACTIVE");

        UserEntity saved = userRepository.save(entity);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        UserEntity entity = userRepository.findById(id);
        if (entity == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "User not found with id: " + id);
        }

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(entity.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(CommonErrorCode.VALIDATION_FAILED, "Email already exists: " + request.getEmail());
            }
            entity.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) entity.setFullName(request.getFullName());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getRole() != null) entity.setRole(request.getRole());

        UserEntity updated = userRepository.save(entity);
        return UserResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
