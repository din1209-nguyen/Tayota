package com.tayota.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationTokenService {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_PREFIX = "verification_token:";
    private static final long EXPIRATION_HOURS = 1;

    // Tạo Token và lưu vào Redis
    public String generateAndSaveToken(String email) {
        String token = UUID.randomUUID().toString();
        String key = TOKEN_PREFIX + email;

        // Lưu token vào Redis với thời gian hết hạn
        redisTemplate.opsForValue().set(key, token, EXPIRATION_HOURS, TimeUnit.HOURS);
        return token;
    }

    // Xác thực Token
    public boolean verifyToken(String email, String token) {
        String key = TOKEN_PREFIX + email;
        String storedToken = redisTemplate.opsForValue().get(key);

        // Kiểm tra Token có tồn tại và khớp với token đã lưu
        return storedToken != null && storedToken.equals(token);
    }

    // Xoá Token khi xác thực thành công
    public void deleteToken(String email) {
        String key = TOKEN_PREFIX + email;
        redisTemplate.delete(key);
    }
}

