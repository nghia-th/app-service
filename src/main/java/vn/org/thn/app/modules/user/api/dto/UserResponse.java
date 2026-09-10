package vn.org.thn.app.modules.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    @Schema(description = "ID người dùng", example = "1")
    private Long id;

    @Schema(description = "Tên đăng nhập", example = "john_doe")
    private String username;

    @Schema(description = "Địa chỉ email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Họ và tên", example = "John Doe")
    private String fullName;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    private String status;

    @Schema(description = "Vai trò", example = "USER")
    private String role;

    public static UserResponse fromEntity(UserEntity entity) {
        if (entity == null) return null;
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .status(entity.getStatus())
                .role(entity.getRole())
                .build();
    }
}
