package com.nguyendin.authservice.controller;

import com.nguyendin.authservice.dto.Request.RegisterRequestDTO;
import com.nguyendin.authservice.service.AuthService;
import com.nguyendin.commoncore.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        authService.register(registerRequestDTO);
        return ApiResponse.success(200, "Tạo thành công, vui lòng xác thực qua email", null);
    }
}
