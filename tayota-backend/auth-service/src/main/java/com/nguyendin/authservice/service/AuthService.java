package com.nguyendin.authservice.service;

import com.nguyendin.authservice.dto.Request.RegisterRequestDTO;
import com.nguyendin.authservice.repository.UserRepository;
import com.nguyendin.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private UserRepository userRepository;

    public void register(RegisterRequestDTO registerRequestDTO) {
        // Trường hơp email đã tồn tại
        if (userRepository.existsByEmail(registerRequestDTO.getEmail())) {
            throw new CustomException(404, "Email already exists!");
        }

        
    }
}
