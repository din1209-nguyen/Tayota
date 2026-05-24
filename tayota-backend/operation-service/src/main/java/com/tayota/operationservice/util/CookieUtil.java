package com.tayota.operationservice.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    // Định nghĩa tên cookie dùng để lưu sessionId của phiên chat
    public static final String CHAT_SESSION_COOKIE = "chat_session";
    public static final int CHAT_SESSION_MAX_AGE_SEC = 24 * 60 * 60;

    // Lưu thời gian hết hạn của refresh-token để set maxAge cho cookie refresh_token
    @Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpirationMs;

    // Xác định cookie có dùng thuộc tính Secure hay không
    @Value("${cookie.secure}")
    private boolean secure;

    @Value("${cookie.same-site:Lax}")
    private String sameSite;

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

    // Thiết lập Refresh Token vào HttpOnly Cookie khi Login hoặc Refresh Token
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        setCookie(response, "refresh_token", refreshToken, (int) (jwtRefreshTokenExpirationMs / 1000));
    }

    // Xóa Refresh Token khỏi Cookie khi Logout
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        // Ghi đè cookie cũ với maxAge = 0 để trình duyệt tự xóa
        setCookie(response, "refresh_token", null, 0);
    }

    // Thiết lập Cookie vào HttpServletResponse
    public void setCookie(HttpServletResponse response, String name, String value, int maxAgeSec) {
        // Chuyển giá trị null thành chuỗi rỗng khi cần xóa cookie
        String cookieValue = (value == null) ? "" : value;

        // Tạo chuỗi Secure nếu bật HTTPS
        String secureAttribute = secure ? "Secure; " : "";

        /* Cấu hình Header thủ công vì HttpServletResponse mặc định không hỗ trợ thuộc tính SameSite */

        // Tạo header Cookie với các thuộc tính bảo mật:
        // Ngăn JavaScript truy cập cookie bằng HttpOnly
        // Chỉ gửi cookie qua HTTPS khi bật Secure
        // Giảm rủi ro CSRF bằng SameSite=Lax
        // Áp dụng cookie cho toàn bộ domain bằng Path=/
        String cookieHeader = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; %sSameSite=%s",
                name,
                cookieValue,
                maxAgeSec,
                secureAttribute,
                sameSite
        );

        // Thêm header Set-Cookie vào response
        response.addHeader("Set-Cookie", cookieHeader);
    }
}
