package com.tayota.userservice.util;

import com.tayota.userservice.model.TokenPair;
import com.tayota.userservice.model.UserSession;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class JwtUtil {
    // Khai báo khoá bí mật SecretKey
    private final SecretKey secretKey;
    // Khai báo biến JwtParser để tái sử dụng
    private final JwtParser jwtParser;

    // Thời gian hết hạn của access-token
    @Value("${jwt.access-token-expiration}")
    private long jwtAccessTokenExpirationMs;
    // Thời gian hết hạn của refresh-token
    @Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpirationMs;

    private final RedisTemplate<String, Object> redisTemplate;

    public JwtUtil(@Value("${jwt.secret}") String secret, RedisTemplate<String, Object> redisTemplate) {
        // Khởi tạo RedisTemplate để lưu trữ và truy xuất session người dùng (refresh-token hash) trong Redis
        this.redisTemplate = redisTemplate;

        // Khoá bí mật được tạo từ chuỗi secret
        secretKey = Keys.hmacShaKeyFor(secret.getBytes());

        // Build JwtParser một lần duy nhất khi khởi động ứng dụng
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
    }

    // Tạo access-token và refresh-token
    public TokenPair generateTokenPair(String userId, List<String> roles, String deviceId, String userAgentString) {
        // Đặt thông tin vào access-token
        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put("role", roles);

        // Tạo access-token
        String accessToken = generateToken(userId, jwtAccessTokenExpirationMs, accessClaims, "access");

        // Đặt thông tin vào refresh-token
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put("role", roles);
        refreshClaims.put("deviceId", deviceId);
        refreshClaims.put("userAgentString", userAgentString);
        // Tạo refresh-token
        String refreshToken = generateToken(userId, jwtRefreshTokenExpirationMs, refreshClaims, "refresh");

        return new TokenPair(accessToken, refreshToken);
    }

    // Tạo chi tiết token
    public String generateToken(String userId, long expirationMs, Map<String, Object> claims, String tokenType) {
        // Thêm kiểu token để phân loại
        claims.put("type", tokenType);

        // Xác định thời gian hiện tại
        Date now = new Date();
        // Xác định thời gian hết hạn
        Date expiryDate = new Date(now.getTime() + expirationMs);

        // Tạo token với thông tin người dùng, claims, thời gian tạo và hết hạn, sau đó ký bằng secretKey
        return Jwts.builder()
                .subject(userId)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // Kiểm tra token hợp lệ và có phải là refresh-token
    public String validateAndExtractDeviceIdFromRefreshToken(String refreshToken) {
        try {
            // Xác thực token và lấy ra thông tin Claims
            Claims claims = getClaims(refreshToken);

            // Kiểm tra token có phải là refresh-token hay không
            String tokenType = claims.get("type", String.class);

            if (!Objects.equals(tokenType, "refresh")) {
                return null;
            }
            return claims.get("deviceId", String.class);
        }
        catch (Exception e) {
            return null;
        }
    }

    // Xác thực Token và lấy dữ liệu từ Payload
    public Claims getClaims(String token) {
        return jwtParser
                // Thực hiện băm lại và đối chiếu chữ ký, kiểm tra thời gian hết hạn (exp) của token
                .parseSignedClaims(token)
                // Trích xuất các thông tin người dùng (Claims) đã được mã hóa trong phần thân của token
                .getPayload();
    }

    // Băm refresh-token bằng SHA-256 để lưu vào Redis (bảo mật hơn so với lưu token gốc)
    public String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }
        catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    // So sánh refresh-token người dùng gửi lên với refresh-token hash đã lưu trong Redis
    // Sử dụng kỹ thuật so sánh thời gian cố định (Constant-time comparison) để chống tấn công Timing Attack
    public boolean compareToRefreshTokenHash(String refreshToken, String sessionKey) {
        // Truy xuất session của người dùng trong Redis
        UserSession savedSession = (UserSession) redisTemplate.opsForValue().get(sessionKey);

        // Trường hợp session tồn tại và chưa hết hạn (TTL) thì mới so sánh hash
        if (savedSession != null) {
            // Lấy refresh-token hash từ session
            String refreshTokenHash = savedSession.getRefreshHash();

            // Băm token người dùng gửi lên bằng cùng thuật toán (SHA-256) để có cùng định dạng với Redis
            String hashedInput = hashRefreshToken(refreshToken);

            // Sử dụng MessageDigest.isEqual để so sánh mảng byte
            // - Không dùng String.equals() vì nó sẽ dừng ngay khi gặp ký tự sai (lộ tốc độ xử lý)
            // - MessageDigest.isEqual() sẽ duyệt qua toàn bộ độ dài của chuỗi bất kể đúng hay sai
            // - Điều này ngăn kẻ tấn công đo thời gian phản hồi của server để đoán mã hash (Timing Attack)
            return MessageDigest.isEqual(
                    hashedInput.getBytes(StandardCharsets.UTF_8),
                    refreshTokenHash.getBytes(StandardCharsets.UTF_8)
            );
        }
        return false;
    }
}
