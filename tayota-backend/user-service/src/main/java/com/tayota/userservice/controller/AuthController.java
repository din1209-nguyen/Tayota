package com.tayota.userservice.controller;

import com.tayota.userservice.dto.Request.LoginRequestDTO;
import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.model.TokenPair;
import com.tayota.userservice.service.AuthService;
import com.tayota.commoncore.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        // Đăng ký tài khoản
        authService.register(registerRequestDTO);

        return ApiResponse.success(200, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.", null);
    }

    @GetMapping("/verify")
    public ApiResponse<Void> verifyEmail(
            @RequestParam(name = "email") String email,
            @RequestParam(name = "token") String token) {
        // Xác thực tài khoản qua email và token
        authService.verify(email, token);

        return ApiResponse.success(200, "Email đã xác thực thành công!", null);
    }

    @PostMapping("login")
    public ApiResponse<Void> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO, HttpServletRequest request, HttpServletResponse response) {
        // Đăng nhập tài khoản
        authService.login(loginRequestDTO, request, response);

        return ApiResponse.success(200, "Đăng nhập thành công!", null);
    }
}
