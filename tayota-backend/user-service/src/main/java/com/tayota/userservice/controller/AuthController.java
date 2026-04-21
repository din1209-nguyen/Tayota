package com.tayota.userservice.controller;

import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.service.AuthService;
import com.tayota.commoncore.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        String message = authService.register(registerRequestDTO);
        return ApiResponse.success(200, message, null);
    }

    @GetMapping("/verify")
    public ApiResponse<Void> verifyEmail(
            @RequestParam(name = "email") String email,
            @RequestParam(name = "token") String token) {

        String message = authService.verify(email, token);
        return ApiResponse.success(200, message, null);
    }
}
