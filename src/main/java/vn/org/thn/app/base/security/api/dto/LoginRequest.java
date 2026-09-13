package vn.org.thn.app.base.security.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username must not be blank")
    @Schema(description = "Tên đăng nhập", example = "john_doe")
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "Mật khẩu đăng nhập", example = "S3curePass!")
    private String password;
}
