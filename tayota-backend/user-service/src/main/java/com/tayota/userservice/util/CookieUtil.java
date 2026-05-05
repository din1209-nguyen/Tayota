package com.tayota.userservice.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
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

    // Thiết lập Refresh Token vào HttpOnly Cookie khi Login hoặc Refresh Token
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        setCookie(response, "refresh_token", refreshToken, (int) (jwtRefreshTokenExpirationMs / 1000));
    }

    // Xóa Refresh Token khỏi Cookie khi Logout
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        // Ghi đè cookie cũ với giá trị null và maxAge = 0 để trình duyệt tự xóa
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
        // SameSite=Lax: Giảm rủi ro CSRF cho refresh-token cookie nhưng vẫn phù hợp với luồng điều hướng thông thường
        // Access-token không lưu cookie; client lưu phía mình và gửi qua header Authorization: Bearer <token>
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
