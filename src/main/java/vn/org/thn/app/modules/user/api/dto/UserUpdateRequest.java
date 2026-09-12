package vn.org.thn.app.modules.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Email(message = "Invalid email format")
    @Schema(description = "Địa chỉ email", example = "john.updated@example.com")
    private String email;

    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    @Schema(description = "Họ và tên", example = "John Doe Updated")
    private String fullName;

    @Schema(description = "Trạng thái (ACTIVE / INACTIVE)", example = "ACTIVE")
    private String status;

    @Schema(description = "Vai trò (ADMIN / USER)", example = "USER")
    private String role;
}
