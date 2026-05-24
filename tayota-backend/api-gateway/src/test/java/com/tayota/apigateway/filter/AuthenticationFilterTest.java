package com.tayota.apigateway.filter;

import com.tayota.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationFilterTest {
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final AuthenticationFilter filter = new AuthenticationFilter(jwtUtil, false, 21600);

    @Test
    void aiChatGuestWithoutCookieCreatesSessionCookieAndHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/ai/api/v1/chat").build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureExchange(forwarded)).block();

        String sessionHeader = forwarded.get().getRequest().getHeaders().getFirst("X-AI-Session-Id");
        assertThat(sessionHeader).isNotBlank();
        assertThat(exchange.getResponse().getCookies().getFirst("ai_session_id")).isNotNull();
        assertThat(exchange.getResponse().getCookies().getFirst("ai_session_id").getValue())
                .isEqualTo(sessionHeader);
        assertThat(exchange.getResponse().getCookies().getFirst("ai_session_id").getMaxAge().getSeconds())
                .isEqualTo(21600);
    }

    @Test
    void aiChatGuestWithCookieReusesSessionAndDoesNotSetNewCookie() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/ai/api/v1/chat")
                        .header(HttpHeaders.COOKIE, "ai_session_id=existing-session")
                        .header("X-AI-Session-Id", "spoofed-session")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureExchange(forwarded)).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-AI-Session-Id"))
                .isEqualTo("existing-session");
        assertThat(exchange.getResponse().getCookies()).doesNotContainKey("ai_session_id");
    }

    @Test
    void aiChatWithValidTokenAddsUserHeaders() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(claims.get("role", List.class)).thenReturn(List.of("CUSTOMER"));
        when(claims.get("email", String.class)).thenReturn("user@example.com");
        when(jwtUtil.getClaims(eq("valid-token"))).thenReturn(claims);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/ai/api/v1/chat")
                        .header(HttpHeaders.COOKIE, "ai_session_id=session-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureExchange(forwarded)).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-AI-Session-Id")).isEqualTo("session-1");
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("user-1");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("CUSTOMER");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("user@example.com");
    }

    @Test
    void aiChatWithInvalidTokenReturnsUnauthorized() {
        when(jwtUtil.getClaims(eq("bad-token"))).thenThrow(new IllegalArgumentException("bad"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/ai/api/v1/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureExchange(forwarded)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getCookies()).doesNotContainKey("ai_session_id");
        assertThat(forwarded.get()).isNull();
    }

    private GatewayFilterChain captureExchange(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }
}
