package com.tayota.userservice.service;

import com.tayota.commoncore.dto.ErrorCode;
import com.tayota.userservice.dto.Request.LoginRequestDTO;
import com.tayota.userservice.dto.Response.TokenPairDTO;
import com.tayota.userservice.grpc.NotificationGrpcClient;
import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.enums.StatusType;
import com.tayota.userservice.mapper.UserMapper;
import com.tayota.userservice.repository.UserRepository;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Authenticator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenService verificationTokenService;
    private final NotificationGrpcClient notificationGrpcClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${api-gate-port:8090}")
    private String apiGatePort;

    // Khởi tạo AuthenticationManager để xác thực người dùng
    private final AuthenticationManager authenticationManager;

    // Khởi tạo JwtUtil để tạo token
    private final JwtUtil jwtUtil;


    // Đăng ký tài khoản người dùng
    public String register(RegisterRequestDTO registerRequestDTO) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(registerRequestDTO.getEmail())) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
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
        // Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra trạng thái người dùng
        if (user.getStatus() == StatusType.ACTIVE) {
            throw new CustomException(403, "Tài khoản đã được xác thực!");
        }

        // Kiểm tra token hợp lệ
        if (!verificationTokenService.verifyToken(email, token)) {
            throw new CustomException(400, "Mã xác thực không hợp lệ!");
        }

        // Cập nhật status thành ACTIVE
        user.setStatus(StatusType.ACTIVE);
        userRepository.save(user);

        // Xoá token khỏi Redis
        verificationTokenService.deleteToken(email);

        // Gửi thông báo xác thực thành công
        notificationGrpcClient.sendRegistrationSuccessEmail(email);

        return "Email đã xác thực thành công!";
    }

    // Đăng nhập tài khoản
    public TokenPairDTO login(LoginRequestDTO loginRequestDTO) {
        // Tạo key cho mỗi email để đếm số lần đăng nhập thất bại
        String redisKey = "auth:login:failed:" + loginRequestDTO.getEmail();
        // Truy xuất số lần nhập sai
        Integer failedCountStr = (Integer) redisTemplate.opsForValue().get(redisKey);

        // Kiểm tra số lần nhập sai vượt quá giới hạn
        if (failedCountStr != null && failedCountStr >= 5) {
            throw new CustomException(403, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau!");
        }

        try {
            // Gửi username/password vào DaoAuthenticationProvider
            // Provider gọi loadUserByUsername() trong CustomUserDetailsService
            // Kiểm tra mật khẩu và trạng thái người dùng
            // So sánh password bằng PasswordEncoder
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword())
            );

            // Xoá số lần nhập sai mật khẩu
            redisTemplate.delete(redisKey);

            // Tạo access-token và refresh-token sau đó trả về
            return jwtUtil.generateTokenPair(authentication);
        }
        // Bắt lỗi khi sai mật khẩu
        catch (BadCredentialsException e) {
            // Tăng số lần nhập sai
            Integer failedCount = failedCountStr == null ? 1 : failedCountStr + 1;
            // Lưu vào Redis
            redisTemplate.opsForValue().set(redisKey, failedCount, Duration.ofMinutes(5));

            // Ném lỗi cả 2 email hoặc mật khẩu vì bảo mật
            throw new CustomException(401, "Email hoặc mật khẩu không đúng!");
        }
    }
}
