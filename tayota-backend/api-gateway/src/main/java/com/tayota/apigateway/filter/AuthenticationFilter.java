package com.tayota.apigateway.filter;

import com.tayota.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
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
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // AntPathMatcher là công cụ của Spring giúp so sánh chuỗi URI có chứa dấu * (wildcard)
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Danh sách các đường dẫn không cần xác thực
    private final List<String> whitelistUrls = List.of(
            "/user/register", "/user/verify", "/user/login", "/user/refresh-token"
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
        if (whitelistUrls.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            return chain.filter(exchange);
        }
        /* access-token từ Header */
        //// Tìm header có tên là "Authorization" (Chứa access-token)
        //String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        //
        //// Nếu header không tồn tại hoặc không bắt đầu bằng chữ "Bearer " -> Báo lỗi 401
        //if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        //    return unAuthorizedResponse(exchange.getResponse(), "Không tìm thấy Token hoặc sai định dạng");
        //}
        //
        //// Cắt bỏ 7 ký tự đầu ("Bearer ") để lấy ra chuỗi access-token
        //String token = authHeader.substring(7);

        /* access-token từ Cookie*/
        String token = request.getCookies()
                .getFirst("access_token") != null
                ? Objects.requireNonNull(request.getCookies().getFirst("access_token")).getValue()
                : null;

        // Không tìm thấy cookie accessToken thì báo lỗi 401
        if (token == null || token.isBlank()) {
            return unAuthorizedResponse(exchange.getResponse(), "Vui lòng đăng nhập!");
        }

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
            log.error("Error validating access token: {}", e.getMessage());
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

    // Xác định thứ tự chạy của Filter này
    // Số càng nhỏ thì Filter càng được chạy sớm
    @Override
    public int getOrder() {
        return -1;
    }
}