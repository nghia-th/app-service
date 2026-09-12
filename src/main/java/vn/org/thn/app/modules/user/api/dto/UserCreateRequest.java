package vn.org.thn.app.modules.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Tên đăng nhập", example = "john_doe")
    private String username;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    @Schema(description = "Địa chỉ email", example = "john.doe@example.com")
    private String email;

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @Schema(description = "Họ và tên", example = "John Doe")
    private String fullName;

    @Schema(description = "Vai trò (ADMIN / USER)", example = "USER")
    private String role;
}
