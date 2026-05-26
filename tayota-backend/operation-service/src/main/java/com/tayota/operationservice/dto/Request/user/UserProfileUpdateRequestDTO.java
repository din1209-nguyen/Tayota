package com.tayota.operationservice.dto.request.user;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserProfileUpdateRequestDTO {
    @NotBlank(message = "Người dùng không hợp lệ")
    private String userId;

    @Size(max = 40, message = "Tên hiển thị không được vượt quá 40 ký tự")
    private String fullname;

    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private Boolean gender;

    @Past(message = "Ngày sinh không hợp lệ")
    private LocalDate birthDate;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Size(max = 1024, message = "Đường dẫn không được vượt quá 1024 ký tự")
    private String avatarUrl;
}
