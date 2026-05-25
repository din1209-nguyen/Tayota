package com.tayota.operationservice.controller.admin;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.request.admin.AdminResetPasswordRequest;
import com.tayota.operationservice.dto.request.admin.AdminUpdateDealershipRequest;
import com.tayota.operationservice.dto.response.admin.AdminUserResponse;
import com.tayota.operationservice.dto.response.car.PaginationResponseDTO;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.service.auth.AuthService;
import com.tayota.operationservice.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService userService;
    private final AuthService authService;

    @GetMapping
    public ApiResponse<PaginationResponseDTO<AdminUserResponse>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false) StatusType status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PaginationResponseDTO<AdminUserResponse> result = userService.searchUsers(keyword, role, status, page, size);
        return ApiResponse.success(200, "Lấy danh sách tài khoản thành công.", result);
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable String userId) {
        AdminUserResponse result = userService.getUserForAdmin(userId);
        return ApiResponse.success(200, "Lấy tài khoản thành công.", result);
    }

    @PatchMapping("/{userId}/password")
    public ApiResponse<Void> resetPassword(
            @PathVariable String userId,
            @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        authService.resetPasswordByAdmin(userId, request);
        return ApiResponse.success(200, "Đặt lại mật khẩu thành công.", null);
    }

    @PatchMapping("/{userId}/dealership")
    public ApiResponse<AdminUserResponse> updateDealership(
            @PathVariable String userId,
            @Valid @RequestBody AdminUpdateDealershipRequest request
    ) {
        AdminUserResponse result = userService.updateDealershipForAdmin(userId, request);
        return ApiResponse.success(200, "Cập nhật đại lý thành công.", result);
    }
}
