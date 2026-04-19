package com.tayota.userservice.controller;

import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.service.AuthService;
import com.tayota.commoncore.dto.ApiResponse;
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
    public ApiResponse<User> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        return ApiResponse.success(200, "Tạo thành công, vui lòng xác thực qua email", authService.register(registerRequestDTO));
    }
}
