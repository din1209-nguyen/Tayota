package com.tayota.userservice.util;

import com.tayota.userservice.dto.Response.TokenPairDTO;
import com.tayota.userservice.entity.CustomUserDetails;
import com.tayota.userservice.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    public JwtUtil(CustomUserDetailsService customUserDetailsService, @Value("${jwt.secret}") String secret) {
        // Khoá bí mật được tạo từ chuỗi secret
        secretKey = Keys.hmacShaKeyFor(secret.getBytes());

        // Build JwtParser một lần duy nhất khi khởi động ứng dụng
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
    }

    // Tạo access-token và refresh-token
    public TokenPairDTO generateTokenPair(Authentication authentication) {
        // Tạo access-token và refresh-token
        String accessToken = generateToken(authentication, jwtAccessTokenExpirationMs, "access");
        String refreshToken = generateToken(authentication, jwtRefreshTokenExpirationMs, "refresh");

        return new TokenPairDTO(accessToken, refreshToken);
    }

    // Tạo chi tiết token
    private String generateToken(Authentication authentication, long expirationMs, String tokenType) {
        // Lấy thông tin người dùng từ Authentication
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Đặt thông tin vào Claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getAuthorities());
        claims.put("tokenType", tokenType);

        // Xác định thời gian hiện tại
        Date now = new Date();
        // Xác định thời gian hết hạn
        Date expiryDate = new Date(now.getTime() + expirationMs);

        // Tạo token với thông tin người dùng, claims, thời gian tạo và hết hạn, sau đó ký bằng secretKey
        return Jwts.builder()
                .subject(String.valueOf(userDetails.getId()))
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // Kiểm tra token hợp lệ và có phải là refresh-token
    public boolean validateRefreshToken(String refreshToken) {
        try {
            // Xác thực token và lấy ra thông tin Claims
            Claims claims = getClaims(refreshToken);

            // Kiểm tra token có phải là refresh-token hay không
            String tokenType = claims.get("tokenType", String.class);
            return "refresh".equals(tokenType);
        }
        catch (Exception e) {
            return false;
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
}
