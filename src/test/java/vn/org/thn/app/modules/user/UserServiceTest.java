package vn.org.thn.app.modules.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.org.thn.app.base.core.exception.BusinessException;
import vn.org.thn.app.modules.user.api.dto.UserCreateRequest;
import vn.org.thn.app.modules.user.api.dto.UserResponse;
import vn.org.thn.app.modules.user.api.dto.UserUpdateRequest;
import vn.org.thn.app.modules.user.application.UserService;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;
import vn.org.thn.app.modules.user.infrastructure.UserRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should create user successfully when username and email do not exist")
    void createUser_validRequest_returnsUserResponse() {
        UserCreateRequest request = new UserCreateRequest("john_doe", "john@example.com", "John Doe", "USER");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> {
            UserEntity entity = i.getArgument(0);
            entity.setId(1L);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setCreatedBy("system");
            entity.setUpdatedBy("system");
            return entity;
        });

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("john_doe", response.getUsername());
        assertEquals("ACTIVE", response.getStatus());

        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when username already exists")
    void createUser_duplicateUsername_throwsBusinessException() {
        UserCreateRequest request = new UserCreateRequest("john_doe", "john@example.com", "John Doe", "USER");

        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessException when email already exists")
    void createUser_duplicateEmail_throwsBusinessException() {
        UserCreateRequest request = new UserCreateRequest("john_doe", "john@example.com", "John Doe", "USER");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update user successfully when user exists")
    void updateUser_userExists_returnsUpdatedUserResponse() {
        UserEntity existing = new UserEntity(1L, "john_doe", "john@example.com", "John Doe", "ACTIVE", "USER");
        UserUpdateRequest request = new UserUpdateRequest("john.new@example.com", "John Updated", "ACTIVE", "ADMIN");

        when(userRepository.findById(1L)).thenReturn(existing);
        when(userRepository.existsByEmail("john.new@example.com")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> {
            UserEntity entity = i.getArgument(0);
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setUpdatedBy("system");
            return entity;
        });

        UserResponse response = userService.updateUser(1L, request);

        assertNotNull(response);
        assertEquals("john.new@example.com", response.getEmail());
        assertEquals("John Updated", response.getFullName());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    @DisplayName("Should throw BusinessException when updating with duplicate email")
    void updateUser_duplicateEmail_throwsBusinessException() {
        UserEntity existing = new UserEntity(1L, "john_doe", "john@example.com", "John Doe", "ACTIVE", "USER");
        UserUpdateRequest request = new UserUpdateRequest("john.other@example.com", "John Updated", "ACTIVE", "ADMIN");

        when(userRepository.findById(1L)).thenReturn(existing);
        when(userRepository.existsByEmail("john.other@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.updateUser(1L, request));

        verify(userRepository, never()).save(any());
    }
}
