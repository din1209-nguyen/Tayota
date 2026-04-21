package com.tayota.userservice.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${jwt.access-token-expiration}")
    private long jwtAccessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpirationMs;

    // Thiết lập Access Token và Refresh Token vào Cookie khi Login hoặc Refresh Toke
    public void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Tạo cookie cho Access Token
        addCookie(response, "access_token", accessToken, (int) (jwtAccessTokenExpirationMs / 1000));
        // Tạo cookie cho Refresh Token
        addCookie(response, "refresh_token", refreshToken, (int) (jwtRefreshTokenExpirationMs / 1000));
    }

    // Xóa các Token khỏi Cookie khi Logout
    public void clearTokenCookies(HttpServletResponse response) {
        // Ghi đè cookie cũ với giá trị null và maxAge = 0 để trình duyệt tự xóa
        deleteCookie(response, "access_token");
        deleteCookie(response, "refresh_token");
    }

    // Thêm Token vào HttpOnly Cookie
    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSec) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);   // Ngăn chặn JavaScript truy cập (Chống XSS)
        cookie.setSecure(false);    // Đặt thành true nếu chạy trên HTTPS
        cookie.setPath("/");        // Áp dụng cho toàn bộ domain
        cookie.setMaxAge(maxAgeSec); // Thời gian sống của cookie
        response.addCookie(cookie);
    }

    // Xóa Cookie bằng cách set MaxAge = 0
    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}