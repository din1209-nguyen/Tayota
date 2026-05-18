package com.tayota.apigateway.filter;

import com.tayota.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    private final JwtUtil jwtUtil;

    // AntPathMatcher là công cụ của Spring giúp so sánh chuỗi URI có chứa dấu * (wildcard)
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Danh sách các đường dẫn không cần xác thực
    private final List<PublicEndpoint> whitelistUrls = List.of(
            PublicEndpoint.of(HttpMethod.POST, "/user/register"),
            PublicEndpoint.of(HttpMethod.POST, "/user/verify-account"),
            PublicEndpoint.of(HttpMethod.POST, "/user/login"),
            PublicEndpoint.of(HttpMethod.POST, "/user/oauth/google"),
            PublicEndpoint.of(HttpMethod.POST, "/user/refresh-token"),
            PublicEndpoint.of(HttpMethod.POST, "/user/forgot-password/send-otp"),
            PublicEndpoint.of(HttpMethod.POST, "/user/forgot-password/verify-otp"),
            PublicEndpoint.of(HttpMethod.PATCH, "/user/forgot-password/reset-password"),

            PublicEndpoint.of(HttpMethod.GET, "/car/catalog/**"),
            PublicEndpoint.of(HttpMethod.GET, "/car/car-styles"),
            PublicEndpoint.of(HttpMethod.GET, "/car/car-styles/**"),
            PublicEndpoint.of(HttpMethod.GET, "/car/car-series"),
            PublicEndpoint.of(HttpMethod.GET, "/car/car-series/**"),
            PublicEndpoint.of(HttpMethod.GET, "/car/car-versions"),
            PublicEndpoint.of(HttpMethod.GET, "/car/car-versions/**"),
            PublicEndpoint.of(HttpMethod.GET, "/car/accessories"),
            PublicEndpoint.of(HttpMethod.GET, "/car/accessories/**")
    );

    // Đây là hàm cốt lõi, mọi request đi qua Gateway đều phải chạy qua hàm này
    // Mono<Void> đại diện cho một tác vụ bất đồng bộ (sẽ hoàn thành trong tương lai)
    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Lấy đối tượng Request (chứa dữ liệu người dùng gửi lên) từ Exchange
        ServerHttpRequest request = exchange.getRequest();

        // Lấy đường dẫn API người dùng đang muốn gọi (VD: /api/users/profile)
        String path = request.getURI().getPath();

        // Duyệt qua danh sách whitelist, nếu đường dẫn hiện tại khớp với bất kỳ pattern nào thì trả về true
        if (HttpMethod.OPTIONS.equals(request.getMethod())
                || whitelistUrls.stream().anyMatch(endpoint -> isWhitelisted(endpoint, request.getMethod(), path))) {
            return chain.filter(exchange);
        }
        
        /* access-token từ header chuẩn Authorization: Bearer <token> */
        // Tìm header có tên là "Authorization" (chứa access-token)
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Nếu header không tồn tại hoặc không đúng chuẩn Bearer thì báo lỗi 401
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unAuthorizedResponse(exchange.getResponse(), "Vui lòng đăng nhập để có thể truy cập!");
        }

        // Cắt bỏ 7 ký tự đầu ("Bearer ") để lấy access-token
        String token = authHeader.substring(7);

        try {
            // Giải mã và xác thực, nếu lỗi sẽ ném xuống catch
            Claims claims = jwtUtil.getClaims(token);

            // Lấy userId và role từ Claim sau khi đuợc giải mã thành công
            String userId = claims.getSubject();
            List<String> roles = claims.get("role", List.class);

            // Nối các role thành chuỗi cách nhau bởi dấu phẩy để gắn vào header
            String roleHeaderValue = (roles != null) ? String.join(",", roles) : "";

            // Request trong Gateway là "Immutable" (Bất biến, không thể sửa đổi trực tiếp)
            // Do đó ta phải dùng mutate() để tạo ra một bản sao mới của Request
            ServerHttpRequest modifiedRequest = request.mutate()
                    //  Xóa các header cũ đi trước để đề phòng Hacker cố tình gửi sẵn header "X-User-Id" để giả mạo (Header Spoofing)
                    .headers(httpHeaders -> {
                        httpHeaders.remove("X-User-Id");
                        httpHeaders.remove("X-User-Role");
                    })
                    // Gắn lại header mới từ chính token đã được xác thực
                    .header("X-User-Id", userId)
                    .header("X-User-Role", roleHeaderValue)
                    .build(); // Hoàn tất tạo bản sao Request

            // Cho phép Request (đã được chỉnh sửa) đi qua Filter này để đến Filter tiếp theo hoặc các Service con
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        }
        catch (ExpiredJwtException e) {
            // Bắt lỗi Token hết hạn để Client gọi API Refresh Token
            return unAuthorizedResponse(exchange.getResponse(), "Access-token đã hết hạn");
        }
        catch (Exception e) {
            // Bắt lỗi trong quá trình giải mã (sai chữ ký, token bị can thiệp, sai thuật toán...)
            return unAuthorizedResponse(exchange.getResponse(), "Access-token không hợp lệ");
        }
    }

    // Xử lý trả về lỗi client
    private Mono<Void> unAuthorizedResponse(ServerHttpResponse response, String message) {
        // Thiết lập lỗi 401 (Unauthorized - Chưa xác thực)
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        // Báo cho Frontend biết đây là chuỗi JSON
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Kết quả sẽ có dạng: {"status": 401, "message": "Lỗi gì đó..."}
        String jsonString = String.format("{\"status\": 401, \"message\": \"%s\"}", message);

        // Đóng gói mảng byte thành DataBuffer (Kiểu dữ liệu đặc thù của Spring WebFlux/Reactive)
        // StandardCharsets.UTF_8 đảm bảo đúng Tiếng Việt
        byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        // Viết dữ liệu vào Response và trả về cho người dùng
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isWhitelisted(PublicEndpoint endpoint, HttpMethod method, String path) {
        return endpoint.method().equals(method) && pathMatcher.match(endpoint.pattern(), path);
    }

    private record PublicEndpoint(HttpMethod method, String pattern) {
        private static PublicEndpoint of(HttpMethod method, String pattern) {
            return new PublicEndpoint(method, pattern);
        }
    }

    // Xác định thứ tự chạy của Filter này
    // Số càng nhỏ thì Filter càng được chạy sớm
    @Override
    public int getOrder() {
        return -1;
    }
}
