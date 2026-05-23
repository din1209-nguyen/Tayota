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


    // Các hằng số và phương thức tiện ích cho cookie của phiên chat, giúp quản lý cookie lưu trữ sessionId của phiên chat giữa khách và hệ thống
    // CHAT_SESSION_COOKIE: Tên cookie dùng để lưu sessionId của phiên chat
    private static final String CHAT_SESSION_COOKIE = "chat-session";
    // CHAT_SESSION_MAX_AGE_SEC: Thời gian sống tối đa của cookie phiên chat (24 giờ), sau đó cookie sẽ tự động hết hạn và bị trình duyệt xóa
    private static final int CHAT_SESSION_MAX_AGE_SEC = 24 * 60 * 60;

    // Thiết lập cookie cho phiên chat với sessionId, giúp duy trì trạng thái phiên chat giữa khách và hệ thống
    // Khi khách bắt đầu một phiên chat mới, hệ thống sẽ tạo một sessionId duy nhất và lưu nó vào cookie để nhận diện phiên chat trong các yêu cầu tiếp theo
    // Cookie này sẽ tồn tại trong 24 giờ hoặc cho đến khi khách đóng trình duyệt, giúp cải thiện trải nghiệm người dùng bằng cách giữ trạng thái phiên chat liên tục
    public void setChatSessionCookie(HttpServletResponse response, String sessionId) {
        // Thiết lập cookie với tên "chat-session", giá trị là sessionId của phiên chat, và thời gian sống tối đa là 24 giờ
        setCookie(response, CHAT_SESSION_COOKIE, sessionId, CHAT_SESSION_MAX_AGE_SEC);
    }

    public void clearChatSessionCookie(HttpServletResponse response) {
        // Xóa cookie phiên chat bằng cách ghi đè với giá trị null và maxAge = 0, yêu cầu trình duyệt xóa cookie ngay lập tức
        setCookie(response, CHAT_SESSION_COOKIE, null, 0);
    }
}
