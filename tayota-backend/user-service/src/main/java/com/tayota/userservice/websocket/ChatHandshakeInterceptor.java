package com.tayota.userservice.websocket;

import com.tayota.userservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
// HandshakeInterceptor để xử lý thông tin xác thực và phiên chat khi thiết lập kết nối WebSocket. 
// Interceptor này sẽ trích xuất thông tin người dùng từ access token nếu có, và thông tin phiên chat từ cookie nếu có, sau đó lưu trữ chúng vào attributes của WebSocket session để sử dụng trong quá trình xử lý tin nhắn.
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtUtil jwtUtil;

    @Override
    // Phương thức trước khi bắt tay để thiết lập kết nối WebSocket. 
    // Trích xuất thông tin người dùng từ access token và thông tin phiên chat từ cookie, sau đó lưu trữ chúng vào attributes của WebSocket session.
    public boolean beforeHandshake(// Kiểm tra nếu request không phải là ServletServerHttpRequest thì bỏ qua interceptor này.
            ServerHttpRequest request,// Kiểm tra nếu request có access token thì giải mã token để lấy thông tin người dùng và lưu vào attributes.
            ServerHttpResponse response,// Kiểm tra nếu request có cookie "chat-session" thì lưu giá trị của cookie đó vào attributes với key "chatSessionId".
            WebSocketHandler wsHandler,// Không sử dụng wsHandler trong phương thức này.
            Map<String, Object> attributes// Trả về true để tiếp tục quá trình bắt tay, bất kể có thông tin người dùng hay phiên chat hay không. Nếu token sai hoặc không có cookie, coi như guest và vẫn cho phép kết nối WebSocket.
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {// Nếu request không phải là ServletServerHttpRequest thì bỏ qua interceptor này.
            return true;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        String accessToken = httpRequest.getParameter("accessToken");
        if (accessToken != null && !accessToken.isBlank()) {// Nếu request có access token thì giải mã token để lấy thông tin người dùng và lưu vào attributes.
            try {
                Claims claims = jwtUtil.getClaims(accessToken);// Giải mã token để lấy thông tin người dùng
                attributes.put("userId", claims.getSubject());// Lưu userId vào attributes
                attributes.put("email", claims.get("email", String.class));// Lưu email vào attributes
                attributes.put("roles", claims.get("role", List.class));// Lưu roles vào attributes
            } catch (Exception ignored) {// Nếu token sai hoặc giải mã thất bại thì bỏ qua, coi như guest và vẫn cho phép kết nối WebSocket.
                // Token sai thì coi như guest. Không chặn guest chat.
            }
        }

        if (httpRequest.getCookies() != null) {
            for (Cookie cookie : httpRequest.getCookies()) {// Duyệt qua các cookie để tìm cookie có tên "chat-session"
                if ("chat-session".equals(cookie.getName())) {// Nếu tìm thấy cookie "chat-session" thì lưu giá trị của cookie đó vào attributes với key "chatSessionId"
                    attributes.put("chatSessionId", cookie.getValue());
                    break;
                }
            }
        }

        return true;
    }

    @Override
    // Phương thức sau khi bắt tay để thiết lập kết nối WebSocket. Không thực hiện bất kỳ hành động nào sau khi kết nối đã được thiết lập.
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}