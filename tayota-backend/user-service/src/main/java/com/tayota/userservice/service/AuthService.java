package com.tayota.userservice.service;

import com.tayota.userservice.grpc.NotificationGrpcClient;
import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.enums.StatusType;
import com.tayota.userservice.mapper.UserMapper;
import com.tayota.userservice.repository.UserRepository;
import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenService verificationTokenService;
    private final NotificationGrpcClient notificationGrpcClient;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${api-gate-port:8090}")
    private String apiGatePort;

    // Đăng ký tài khoản người dùng
    public String register(RegisterRequestDTO registerRequestDTO) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(registerRequestDTO.getEmail())) {
            throw new CustomException(409, "Email already exists!");
        }

        try {
            // Băm password
            String newPasswordHash = passwordEncoder.encode(registerRequestDTO.getPassword());

            // Tạo user với status UNVERIFIED
            User user = UserMapper.toEntity(registerRequestDTO, newPasswordHash);
            user.setStatus(StatusType.UNVERIFIED);

            // Lưu user vào database
            User savedUser = userRepository.save(user);

            // Tạo verification token
            String token = verificationTokenService.generateAndSaveToken(savedUser.getEmail());

            // Tạo verification link
            String verificationLink = frontendUrl + apiGatePort + "/verify?email=" + savedUser.getEmail() + "&token=" + token;

            // Gửi email xác thực qua gRPC (bất đồng bộ - không chặn quá trình đăng ký)
            notificationGrpcClient.sendVerificationEmail(savedUser.getEmail(), verificationLink);

            // Trả về tin nhắn thành công
            return "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.";
        }
        catch (Exception e) {
            throw new CustomException(500, "Registration failed: " + e.getMessage());
        }
    }

    // Xác thực tài khoản qua email và token
    public String verify(String email, String token) {
        // Kiểm tra token hợp lệ
        if (!verificationTokenService.verifyToken(email, token)) {
            throw new CustomException(400, "Token không hợp lệ!");
        }

        // Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy người dùng!"));

        // Cập nhật status thành ACTIVE
        user.setStatus(StatusType.ACTIVE);
        userRepository.save(user);

        // Xoá token khỏi Redis
        verificationTokenService.deleteToken(email);

        // Gửi thông báo xác thực thành công
        notificationGrpcClient.sendRegistrationSuccessEmail(email);

        return "Email đã xác thực thành công!";
    }
}
