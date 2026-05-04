package com.tayota.commoncore.util;

import com.tayota.commoncore.enums.RoleType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public final class SecurityUtil {
    // Map lưu level của từng role trong hệ thống phân cấp
    public static final Map<String, Integer> ROLE_HIERARCHY_MAP =
            Map.of(
                    "ROLE_ADMIN", 5,
                    "ROLE_MANAGER", 4,
                    "ROLE_ASSISTANT", 1,
                    "ROLE_MECHANIC", 1,
                    "ROLE_USER", 1
            );

    // Lấy userId hiện tại từ SecurityContext đã được set trong filter
    public static String getCurrentUserId() {
        // Lấy Authentication từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Nếu chưa có authentication thì return null
        if (authentication == null) {
            throw new IllegalStateException("Không tìm thấy thông tin người dùng trong SecurityContext");
        }

        // Trả về userId
        return (String) authentication.getPrincipal();
    }

    // Lấy role của user hiện tại từ SecurityContext đã được set trong filter
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("Không tìm thấy thông tin người dùng trong SecurityContext");
        }

        // Lấy authority đầu tiên (Spring Security chỉ set 1 role)
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role của người dùng"));
    }
}