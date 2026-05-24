package com.tayota.apigateway.filter;

import com.tayota.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
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
    private static final String GATEWAY_SECRET = "test-gateway-secret";

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final AuthenticationFilter filter = new AuthenticationFilter(jwtUtil, false, 21600, GATEWAY_SECRET);

    @Test
    void aiChatGuestWithoutCookieCreatesSessionCookieAndHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/ai/api/v1/chat").build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureExchange(forwarded)).block();

        String sessionHeader = forwarded.get().getRequest().getHeaders().getFirst("X-AI-Session-Id");
        assertThat(sessionHeader).isNotBlank();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Gateway-Secret"))
                .isEqualTo(GATEWAY_SECRET);
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
                        .cookie(new HttpCookie("ai_session_id", "existing-session"))
                        .header("X-AI-Session-Id", "spoofed-session")
                        .header("X-Gateway-Secret", "spoofed-secret")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureExchange(forwarded)).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-AI-Session-Id"))
                .isEqualTo("existing-session");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Gateway-Secret"))
                .isEqualTo(GATEWAY_SECRET);
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
                        .cookie(new HttpCookie("ai_session_id", "session-1"))
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
        assertThat(headers.getFirst("X-Gateway-Secret")).isEqualTo(GATEWAY_SECRET);
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

    @Test
    void guestAppointmentRoutesArePublicThroughGatewayPaths() {
        assertPublicRoute(MockServerHttpRequest.post("/operation/appointments/test-drive/guest").build());
        assertPublicRoute(MockServerHttpRequest.post("/operation/appointments/service/guest").build());
        assertPublicRoute(MockServerHttpRequest.get("/operation/appointments/available-slots").build());
    }

    @Test
    void reviewTokenRoutesArePublic() {
        assertPublicRoute(MockServerHttpRequest.get("/operation/reviews/token/review-token").build());
        assertPublicRoute(MockServerHttpRequest.patch("/operation/reviews/token/review-token").build());
    }

    @Test
    void protectedRouteWithoutTokenReturnsUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/operation/appointments/my").build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureExchange(forwarded)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void aiHealthIsPublicAndReceivesGatewaySecret() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/ai/health")
                        .header("X-Gateway-Secret", "spoofed-secret")
                        .header("X-User-Id", "spoofed-user")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureExchange(forwarded)).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(headers.getFirst("X-Gateway-Secret")).isEqualTo(GATEWAY_SECRET);
        assertThat(headers.getFirst("X-User-Id")).isNull();
    }

    private GatewayFilterChain captureExchange(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private void assertPublicRoute(MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureExchange(forwarded)).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(forwarded.get()).isNotNull();
    }
}
