package com.tayota.userservice.controller;

import com.tayota.userservice.dto.Request.LoginRequestDTO;
import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.dto.Response.TokenPairDTO;
import com.tayota.userservice.service.AuthService;
import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.userservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        // Đăng ký tài khoản người dùng
        String message = authService.register(registerRequestDTO);

        return ApiResponse.success(200, message, null);
    }

    @GetMapping("/verify")
    public ApiResponse<Void> verifyEmail(
            @RequestParam(name = "email") String email,
            @RequestParam(name = "token") String token) {
        // Xác thực tài khoản qua email và token
        String message = authService.verify(email, token);

        return ApiResponse.success(200, message, null);
    }

    @PostMapping("login")
    public ApiResponse<Void> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO, HttpServletResponse response) {
        // Xác thực và nhận về 2 token từ Service
        TokenPairDTO tokens = authService.login(loginRequestDTO);

        // Gắn tokens vào Cookie qua CookieUtil
        cookieUtil.setTokenCookies(response, tokens.getAccessToken(), tokens.getRefreshToken());

        return ApiResponse.success(200, "Đăng nhập thành công!", null);
    }
}
