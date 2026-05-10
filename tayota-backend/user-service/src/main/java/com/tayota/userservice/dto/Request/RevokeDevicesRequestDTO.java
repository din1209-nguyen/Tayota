package com.tayota.userservice.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class RevokeDevicesRequestDTO {
    @NotBlank(message = "Người dùng không hợp lệ")
    private String userId;

    @NotNull(message = "Danh sách thiết bị không được để trống")
    private List<String> deviceIds;
}
