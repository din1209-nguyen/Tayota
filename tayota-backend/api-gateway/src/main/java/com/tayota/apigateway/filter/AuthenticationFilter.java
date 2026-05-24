package com.tayota.apigateway.filter;

import com.tayota.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    private static final String AI_CHAT_PATH = "/ai/api/v1/chat";
    private static final String AI_PATH_PREFIX = "/ai/";
    private static final String AI_SESSION_COOKIE = "ai_session_id";
    private static final String AI_SESSION_HEADER = "X-AI-Session-Id";
    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    private final JwtUtil jwtUtil;
    private final boolean aiChatCookieSecure;
    private final long aiChatSessionTtlSeconds;
    private final String gatewayInternalSecret;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthenticationFilter(
            JwtUtil jwtUtil,
            @Value("${ai.chat.cookie.secure:false}") boolean aiChatCookieSecure,
            @Value("${ai.chat.session-ttl-seconds:21600}") long aiChatSessionTtlSeconds,
            @Value("${gateway.internal-secret:change-me-gateway-internal-secret}") String gatewayInternalSecret) {
        this.jwtUtil = jwtUtil;
        this.aiChatCookieSecure = aiChatCookieSecure;
        this.aiChatSessionTtlSeconds = aiChatSessionTtlSeconds;
        this.gatewayInternalSecret = gatewayInternalSecret;
    }

    private final List<PublicEndpoint> whitelistUrls = List.of(
            PublicEndpoint.of(HttpMethod.POST, "/user/register"),
            PublicEndpoint.of(HttpMethod.POST, "/user/verify-account"),
            PublicEndpoint.of(HttpMethod.POST, "/user/login"),
            PublicEndpoint.of(HttpMethod.POST, "/user/oauth/google"),
            PublicEndpoint.of(HttpMethod.POST, "/user/refresh-token"),
            PublicEndpoint.of(HttpMethod.POST, "/user/forgot-password/send-otp"),
            PublicEndpoint.of(HttpMethod.POST, "/user/forgot-password/verify-otp"),
            PublicEndpoint.of(HttpMethod.PATCH, "/user/forgot-password/reset-password"),

            PublicEndpoint.of(HttpMethod.GET, "/user/chat/ws/**"),
            PublicEndpoint.of(HttpMethod.POST, "/user/chat/sessions/current"),
            PublicEndpoint.of(HttpMethod.GET, "/user/chat/sessions/current/messages"),
            PublicEndpoint.of(HttpMethod.POST, "/user/chat/messages"),

            PublicEndpoint.of(HttpMethod.GET, "/car/catalog/**"),
            PublicEndpoint.of(HttpMethod.GET, "/car/car-styles/**"),
            PublicEndpoint.of(HttpMethod.GET, "/car/car-series/**"),
            PublicEndpoint.of(HttpMethod.GET, "/car/car-versions/**"),
            PublicEndpoint.of(HttpMethod.GET, "/car/accessories/**"),

            PublicEndpoint.of(HttpMethod.GET, "/operation/appointments/available-slots"),
            PublicEndpoint.of(HttpMethod.POST, "/operation/appointments/test-drive/guest"),
            PublicEndpoint.of(HttpMethod.POST, "/operation/appointments/service/guest"),

            PublicEndpoint.of(HttpMethod.GET, "/operation/reviews/token/*"),
            PublicEndpoint.of(HttpMethod.PATCH, "/operation/reviews/token/*"),

            PublicEndpoint.of(HttpMethod.GET, "/ai/health")
    );

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isAiChatRequest(request.getMethod(), path)) {
            return handleAiChat(exchange, chain);
        }

        if (HttpMethod.OPTIONS.equals(request.getMethod())
                || whitelistUrls.stream().anyMatch(endpoint -> isWhitelisted(endpoint, request.getMethod(), path))) {
            return chain.filter(exchange.mutate().request(withSanitizedTrustedHeaders(request, path)).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unAuthorizedResponse(exchange.getResponse(), "Vui long dang nhap de co the truy cap!");
        }

        try {
            ServerHttpRequest modifiedRequest = withAuthenticatedUserHeaders(
                    request,
                    jwtUtil.getClaims(authHeader.substring(7))
            );
            modifiedRequest = withGatewaySecretIfAiRequest(modifiedRequest, path);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }
        catch (ExpiredJwtException e) {
            return unAuthorizedResponse(exchange.getResponse(), "Access-token da het han");
        }
        catch (Exception e) {
            return unAuthorizedResponse(exchange.getResponse(), "Access-token khong hop le");
        }
    }

    private Mono<Void> handleAiChat(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String sessionId = getAiSessionId(request);
        boolean shouldSetSessionCookie = false;
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            shouldSetSessionCookie = true;
        }
        final String resolvedSessionId = sessionId;

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            if (shouldSetSessionCookie) {
                response.addCookie(buildAiSessionCookie(resolvedSessionId));
            }
            ServerHttpRequest modifiedRequest = request.mutate()
                    .headers(this::removeTrustedHeaders)
                    .headers(headers -> headers.set(GATEWAY_SECRET_HEADER, gatewayInternalSecret))
                    .header(AI_SESSION_HEADER, resolvedSessionId)
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        if (!authHeader.startsWith("Bearer ")) {
            return unAuthorizedResponse(response, "Access-token khong hop le");
        }

        try {
            Claims claims = jwtUtil.getClaims(authHeader.substring(7));
            if (shouldSetSessionCookie) {
                response.addCookie(buildAiSessionCookie(resolvedSessionId));
            }
            ServerHttpRequest modifiedRequest = withAuthenticatedUserHeaders(
                    request,
                    claims
            ).mutate()
                    .headers(headers -> {
                        headers.remove(AI_SESSION_HEADER);
                        headers.set(GATEWAY_SECRET_HEADER, gatewayInternalSecret);
                        headers.set(AI_SESSION_HEADER, resolvedSessionId);
                    })
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }
        catch (ExpiredJwtException e) {
            return unAuthorizedResponse(response, "Access-token da het han");
        }
        catch (Exception e) {
            return unAuthorizedResponse(response, "Access-token khong hop le");
        }
    }

    private ServerHttpRequest withAuthenticatedUserHeaders(ServerHttpRequest request, Claims claims) {
        String userId = claims.getSubject();
        List<String> roles = claims.get("role", List.class);
        String email = claims.get("email", String.class);
        String roleHeaderValue = roles != null ? String.join(",", roles) : "";

        return request.mutate()
                .headers(headers -> {
                    removeTrustedHeaders(headers);
                    if (userId != null && !userId.isBlank()) {
                        headers.set(USER_ID_HEADER, userId);
                    }
                    headers.set(USER_ROLE_HEADER, roleHeaderValue);
                    if (email != null && !email.isBlank()) {
                        headers.set(USER_EMAIL_HEADER, email);
                    }
                })
                .build();
    }

    private String getAiSessionId(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst(AI_SESSION_COOKIE);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return null;
        }
        return cookie.getValue();
    }

    private ResponseCookie buildAiSessionCookie(String sessionId) {
        return ResponseCookie.from(AI_SESSION_COOKIE, sessionId)
                .httpOnly(true)
                .secure(aiChatCookieSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(aiChatSessionTtlSeconds))
                .build();
    }

    private void removeTrustedHeaders(HttpHeaders httpHeaders) {
        httpHeaders.remove(USER_ID_HEADER);
        httpHeaders.remove(USER_ROLE_HEADER);
        httpHeaders.remove(USER_EMAIL_HEADER);
        httpHeaders.remove(AI_SESSION_HEADER);
        httpHeaders.remove(GATEWAY_SECRET_HEADER);
    }

    private Mono<Void> unAuthorizedResponse(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonString = String.format("{\"status\": 401, \"message\": \"%s\"}", message);
        byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isWhitelisted(PublicEndpoint endpoint, HttpMethod method, String path) {
        return endpoint.method().equals(method) && pathMatcher.match(endpoint.pattern(), path);
    }

    private boolean isAiChatRequest(HttpMethod method, String path) {
        return HttpMethod.POST.equals(method) && AI_CHAT_PATH.equals(path);
    }

    private ServerHttpRequest withSanitizedTrustedHeaders(ServerHttpRequest request, String path) {
        return request.mutate()
                .headers(headers -> {
                    removeTrustedHeaders(headers);
                    if (path.startsWith(AI_PATH_PREFIX)) {
                        headers.set(GATEWAY_SECRET_HEADER, gatewayInternalSecret);
                    }
                })
                .build();
    }

    private ServerHttpRequest withGatewaySecretIfAiRequest(ServerHttpRequest request, String path) {
        if (!path.startsWith(AI_PATH_PREFIX)) {
            return request;
        }
        return request.mutate()
                .headers(headers -> headers.set(GATEWAY_SECRET_HEADER, gatewayInternalSecret))
                .build();
    }

    private record PublicEndpoint(HttpMethod method, String pattern) {
        private static PublicEndpoint of(HttpMethod method, String pattern) {
            return new PublicEndpoint(method, pattern);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
