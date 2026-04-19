package com.tayota.authservice.service;

import com.tayota.authservice.dto.Request.RegisterRequestDTO;
import com.tayota.authservice.entity.User;
import com.tayota.authservice.mapper.UserMapper;
import com.tayota.authservice.repository.UserRepository;
import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public User register(RegisterRequestDTO registerRequestDTO) {
        // Trường hơp email đã tồn tại
        if (userRepository.existsByEmail(registerRequestDTO.getEmail())) {
            throw new CustomException(404, "Email already exists!");
        }

        // Băm password trước khi lưu
        String newPasswordHash = passwordEncoder.encode(registerRequestDTO.getPassword());

        // Tạo và lưu User vào trong csdl
        return userRepository.save(UserMapper.toEntity(registerRequestDTO, newPasswordHash));
    }
}
