package com.tayota.commoncore.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Không @Component ở đây vì ta sẽ khởi tạo nó bằng @Bean trong file Config, để đảm bảo tính đóng gói của thư viện AutoConfiguration
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    // Tạo các key của Header mà Gateway sẽ gắn vào request
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Lấy thông tin từ Header do API Gateway truyền xuống
        String userId = request.getHeader(USER_ID_HEADER);
        String roleHeader = request.getHeader(USER_ROLE_HEADER);

        // Kiểm tra nếu có userId (nghĩa là đã qua Gateway) VÀ Context hiện tại chưa có ai đăng nhập
        if (StringUtils.hasText(userId) && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Khởi tạo List rỗng
            List<GrantedAuthority> authorities = Collections.emptyList();

            // Nếu có role khi đã đăng nhập
            if (StringUtils.hasText(roleHeader)) {
                // Nếu có role, tách chuỗi role thành mảng bởi dấy phẩy
                String[] roleNames = roleHeader.split(",");

                // Khởi tạo List authorities
                authorities = new ArrayList<>(roleNames.length);

                // Duyệt qua từng role
                for (String rawRole : roleNames) {
                    // Loại bỏ khoảng trắng thừa
                    String role = rawRole.trim();

                    if (!role.isEmpty()) {
                        // Spring Security bắt buộc role phải có tiền tố "ROLE_". Nếu chưa có thì thêm vào
                        if (!role.startsWith("ROLE_")) {
                            role = "ROLE_" + role;
                        }

                        // Chuyển role thành Object mà Spring Security hiểu được
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                }
            }

            // Tạo một đối tượng Authentication cho Spring Security, đại diện cho một user đã được xác thực
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,      // Principal: Định danh người dùng (thường lưu ID hoặc Username)
                    null,        // Credentials: Mật khẩu (đặt null vì chỉ dùng login ban đầu, còn lại thì không cần password)
                    authorities  // Danh sách quyền
            );

            System.out.println("HeaderAuthenticationFilter - userId: " + userId + ", role: " + roleHeader);

            //// Thêm thông tin chi tiết của request vào Authentication, chứa địa chỉ IP của client, session ID dùng để log
            //authentication.setDetails(
            //        new WebAuthenticationDetailsSource().buildDetails(request)
            //);

            // Lưu thông tin Authentication vào SecurityContext và đánh dấu request hiện tại đã đăng nhập
            // Các file khác có thể gọi hàm để lấy userId, hoặc dùng @PreAuthorize để check quyền
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // Chuyển request sang filter bước tiếp theo (nếu có) hoặc vào tầng ứng dụng
        filterChain.doFilter(request, response);
    }
}
