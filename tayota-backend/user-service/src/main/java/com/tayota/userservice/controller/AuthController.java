package com.tayota.userservice.controller;

import com.tayota.commoncore.dto.ErrorCode;
import com.tayota.userservice.dto.Request.LoginRequestDTO;
import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.dto.Response.DeviceResponseDTO;
import com.tayota.userservice.service.AuthService;
import com.tayota.commoncore.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private static final Logger log = LogManager.getLogger(AuthController.class);
    private final AuthService authService;

    // Đăng ký tài khoản
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        authService.register(registerRequestDTO);
        return ApiResponse.success(200, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.", null);
    }

    // Xác thực tài khoản qua  token
    @GetMapping("/verify")
    public ApiResponse<Void> verifyEmail(
            @RequestParam(name = "email") @Email(message = "Email không đúng định dạng") String email,
            @RequestParam(name = "token") @NotBlank(message = "Token không hợp lệ") String token
    ) {

        authService.verify(email, token);
        return ApiResponse.success(200, "Email đã xác thực thành công!", null);
    }

    // Đăng nhập tài khoản
    @PostMapping("login")
    public ApiResponse<Void> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO, HttpServletRequest request, HttpServletResponse response) {
        authService.login(loginRequestDTO, request, response);
        return ApiResponse.success(200, "Đăng nhập thành công!", null);
    }

    // Làm mới access-token
    @PostMapping("refresh-token")
    public ApiResponse<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        authService.refreshToken(request, response);
        return ApiResponse.success(200, "Làm mới token thành công!", null);
    }

    // Đăng xuất tài khoản
    @PostMapping("logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ApiResponse.success(200, "Đăng xuất thành công!", null);
    }

    // Đăng xuất tất cả tài khoản trừ thiết bị hiện tại
    @PostMapping("logout-all")
    public ApiResponse<Void> logoutAll(HttpServletRequest request, HttpServletResponse response) {
        authService.logoutAll(request, response);
        return ApiResponse.success(200, "Đăng xuất tất cả các thiết bị thành công!", null);
    }

    // Lấy danh sách thiết bị đã đăng nhập của một tài khoản
    @GetMapping("/devices/{userId}")
    public ApiResponse<List<DeviceResponseDTO>> getDevices(@PathVariable String userId) {
        List<DeviceResponseDTO> devices = authService.getDevices(userId);
        return ApiResponse.success(200, "Lấy danh sách thiết bị thành công!", devices);
    }

    // Thu hồi quyền truy cập của một thiết bị
    @DeleteMapping("/revoke/{userId}/{deviceId}")
    public ApiResponse<Void> revokeDevice(@PathVariable String userId, @PathVariable String deviceId, HttpServletRequest request) {
        authService.revokeDevice(userId, deviceId, request);
        return ApiResponse.success(200, "Thiết bị đã được thu hồi thành công!", null);
    }
}
