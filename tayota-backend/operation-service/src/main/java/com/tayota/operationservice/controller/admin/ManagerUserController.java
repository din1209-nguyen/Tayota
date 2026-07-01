package com.tayota.operationservice.controller.admin;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.response.admin.AdminUserResponse;
import com.tayota.operationservice.dto.response.admin.ManagerUserStatsResponse;
import com.tayota.operationservice.dto.response.car.PaginationResponseDTO;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/manager/users")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerUserController {
    private final UserService userService;

    @GetMapping
    public ApiResponse<PaginationResponseDTO<AdminUserResponse>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false) StatusType status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(200, "Lấy danh sách người dùng thành công.",
                userService.searchUsersForManager(keyword, role, status, page, size));
    }

    @GetMapping("/stats")
    public ApiResponse<ManagerUserStatsResponse> getStats() {
        return ApiResponse.success(200, "Lấy thống kê người dùng thành công.", userService.getManagerUserStats());
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable String userId) {
        return ApiResponse.success(200, "Lấy thông tin người dùng thành công.", userService.getUserForManager(userId));
    }
}
