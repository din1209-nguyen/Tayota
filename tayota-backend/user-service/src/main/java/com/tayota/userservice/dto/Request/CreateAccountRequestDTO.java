package com.tayota.userservice.dto.Request;

import com.tayota.commoncore.enums.RoleType;
import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class CreateAccountRequestDTO {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email;

    @Size(min = 8, max = 20, message = "Mật khẩu phải từ 8 đến 20 ký tự")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
            message = "Mật khẩu phải bao gồm ít nhất 1 chữ hoa, 1 số và 1 ký tự đặc biệt"
    )
    String password;

    @NotBlank(message = "Vai trò không được để trống")
    String role;
}
