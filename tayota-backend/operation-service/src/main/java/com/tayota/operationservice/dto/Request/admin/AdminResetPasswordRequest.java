package com.tayota.operationservice.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminResetPasswordRequest {
    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 8, max = 20, message = "Mat khau phai tu 8 den 20 ky tu")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
            message = "Mat khau phai co it nhat 1 chu hoa, 1 so va 1 ky tu dac biet"
    )
    private String password;
}
