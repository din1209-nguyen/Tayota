package com.tayota.userservice.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    // Thời gian hết hạn của access-token, dùng để set maxAge cho cookie access_token
    @Value("${jwt.access-token-expiration}")
    private long jwtAccessTokenExpirationMs;

    // Thời gian hết hạn của refresh-token, dùng để set maxAge cho cookie refresh_token
    @Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpirationMs;

    // Dùng cho Https nếu true, ngược lại Http
    @Value("${cookie.secure}")
    private boolean secure;

    // Lấy giá trị của cookie theo tên từ Request
    public String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // Thiết lập Access Token và Refresh Token vào Cookie khi Login hoặc Refresh Toke
    public void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Tạo cookie cho Access Token
        setCookie(response, "access_token", accessToken, (int) (jwtAccessTokenExpirationMs / 1000));
        // Tạo cookie cho Refresh Token
        setCookie(response, "refresh_token", refreshToken, (int) (jwtRefreshTokenExpirationMs / 1000));
    }

    // Xóa các Token khỏi Cookie khi Logout
    public void clearTokenCookies(HttpServletResponse response) {
        // Ghi đè cookie cũ với giá trị null và maxAge = 0 để trình duyệt tự xóa
        setCookie(response, "access_token", null, 0);
        setCookie(response, "refresh_token", null, 0);
    }

    // Thiết lập Cookie vào HttpServletResponse
    private void setCookie(HttpServletResponse response, String name, String value, int maxAgeSec) {
        // Nếu value null → dùng chuỗi rỗng (cần khi xóa cookie)
        String cookieValue = (value == null) ? "" : value;

        // Tạo chuỗi Secure nếu bật HTTPS
        String secureAttribute = secure ? "Secure; " : "";

        /* Vì HttpServletResponse mặc định không hỗ trợ thuộc tính SameSite nên phải cấu hình Header thủ công */

        // Tạo header Cookie với các thuộc tính bảo mật:
        // HttpOnly: Ngăn JavaScript truy cập cookie (chống XSS)
        // Secure: Chỉ gửi cookie qua HTTPS (khuyến nghị bật ở production)
        // SameSite=Strict: Ngăn cookie gửi từ site khác (chống CSRF)
        // SameSite=Lax: Chỉ gửi cookie với các request điều hướng thông thường (GET), giúp chống CSRF nhưng vẫn cho phép click link từ email hoặc site khác
        // Path=/: Áp dụng cookie cho toàn bộ domain
        String cookieHeader = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; %sSameSite=Lax",
                name,
                cookieValue,
                maxAgeSec,
                secureAttribute
        );

        // Thêm header Set-Cookie vào response
        response.addHeader("Set-Cookie", cookieHeader);
    }
}