package com.tayota.operationservice.config.websocket;

import com.tayota.operationservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
    private final CookieUtil cookieUtil;

    // Định nghĩa key attributes của WebSocket session
    private static final String CHAT_SESSION_ATTRIBUTE = "chat_session";

    // Xử lý thông tin phiên chat trước khi thiết lập kết nối WebSocket
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return true;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        resolveChatSession(httpRequest, attributes);

        return true;
    }

    // Lấy chat_session từ cookie và lưu vào attributes
    private void resolveChatSession(HttpServletRequest request, Map<String, Object> attributes) {
        String chatSession = cookieUtil.getCookieValue(request, CookieUtil.CHAT_SESSION_COOKIE);

        if (StringUtils.hasText(chatSession)) {
            attributes.put(CHAT_SESSION_ATTRIBUTE, chatSession);
        }
    }

    // Bỏ qua xử lý sau khi thiết lập kết nối WebSocket
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}
