package com.tayota.userservice.service;

import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.dto.Request.GoogleLoginRequestDTO;
import com.tayota.userservice.dto.Response.AccessTokenResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {
    public AccessTokenResponseDTO login(
            GoogleLoginRequestDTO googleLoginRequestDTO,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new CustomException(400, "Google login chưa được cấu hình");
    }
}
