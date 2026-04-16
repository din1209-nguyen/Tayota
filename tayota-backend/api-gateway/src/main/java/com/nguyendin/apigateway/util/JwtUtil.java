package com.nguyendin.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/*
    JWT có dạng HEADER.PAYLOAD.SIGNATURE được mã hoá Base64 Encode (chống đọc trực tiếp)
    - HEADER: {
          "alg": "thuật toán" (HS256, RS256,...)
          "typ": "kiểu token" (JWT)
      }
    - Payload: chứa thông tin token (Claims)
    - SIGNATURE
        + Được tạo bằng cách: HMACSHA256(
            base64UrlEncode(header) + "." +
            base64UrlEncode(payload),
            secret_key
        )
        + Với HMACSHA256 là thuật toán trong alg (HS256 <=> HMAC-SHA256)
*/

@Slf4j
@Component
public class JwtUtil {
    // Khai báo biến JwtParser để tái sử dụng
    private final JwtParser jwtParser;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // Khởi tạo key từ chuỗi secret
        // Chuyển secret (getBytes) thành mảng byte[] vì thuật toán yêu cầu
        //  HS256	>= 32 bytes - ký tự
        //  HS384	>= 48 bytes - ký tự
        //  HS512	>= 64 bytes - ký tự
        // Khoá bí mật được tạo từ chuỗi secret
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        // Build JwtParser một lần duy nhất khi khởi động ứng dụng
        this.jwtParser = Jwts.parser()
                .verifyWith(key)
                .build();
    }

    // Xác thực Token và lấy dữ liệu từ Payload
    public Claims getClaims(String token) {
        // Hàm parseSignedClaims sẽ tự động ném ra các Exception tương ứng nếu token có vấn đề:
        // - ExpiredJwtException: Nếu token đã hết hạn (Filter sẽ bắt lỗi này để báo Client)
        // - SignatureException: Nếu chữ ký không khớp (bị sửa đổi)
        // - MalformedJwtException: Nếu token sai cấu trúc (không phải chuỗi JWT hợp lệ)
        // ...

        return jwtParser
                // Thực hiện băm lại và đối chiếu chữ ký, kiểm tra thời gian hết hạn (exp) của token
                .parseSignedClaims(token)
                // Trích xuất các thông tin người dùng (Claims) đã được mã hóa trong phần thân của token
                .getPayload();
    }
}
