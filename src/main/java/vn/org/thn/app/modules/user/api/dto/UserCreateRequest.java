package vn.org.thn.app.modules.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @Schema(description = "Tên đăng nhập", example = "john_doe")
    private String username;

    @Schema(description = "Địa chỉ email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Họ và tên", example = "John Doe")
    private String fullName;

    @Schema(description = "Vai trò (ADMIN / USER)", example = "USER")
    private String role;
}
