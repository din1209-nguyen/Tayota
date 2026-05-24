package com.tayota.operationservice.controller.user;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.service.user.UserService;
import lombok.RequiredArgsConstructor;
import com.tayota.operationservice.dto.response.user.BasicInformationResponseDTO;
import com.tayota.operationservice.dto.request.user.UserProfileUpdateRequestDTO;
import com.tayota.operationservice.dto.response.user.UserProfileResponseDTO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // Lấy thông tin cơ bản
    @GetMapping("/me")
    public ApiResponse<BasicInformationResponseDTO> getBasicInformation() {
        BasicInformationResponseDTO basicInformation = userService.getBasicInformation();
        return ApiResponse.success(200, "Lấy thông tin cơ bản thành công.", basicInformation);
    }

    // Lấy hồ sơ
    @GetMapping("/profile/{userId}")
    public ApiResponse<UserProfileResponseDTO> getProfile(@PathVariable String userId) {
        UserProfileResponseDTO userProfile = userService.getProfile(userId);
        return ApiResponse.success(200, "Lấy hồ sơ thành công.", userProfile);
    }

    // Cập nhật hồ sơ
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UserProfileUpdateRequestDTO userProfileUpdateRequestDTO) {
        userService.updateProfile(userProfileUpdateRequestDTO);
        return ApiResponse.success(200, "Cập nhật hồ sơ thành công.", null);
    }
}
