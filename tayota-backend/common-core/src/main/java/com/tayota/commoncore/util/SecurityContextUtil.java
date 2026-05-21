package com.tayota.commoncore.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.Map;

public final class SecurityContextUtil {
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

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("Không tìm thấy thông tin người dùng trong SecurityContext");
        }

        Object details = authentication.getDetails();

        if (!(details instanceof String email) || !StringUtils.hasText(email)) {
            throw new IllegalStateException("Không tìm thấy email người dùng trong SecurityContext");
        }

        return email;
    }

    // Kiểm tra xem role của user hiện tại có cao hơn role của user mục tiêu
    public static boolean validateRoleSuperiority(String currentUserRole, String targetUserRole) {
        // Chuẩn hóa chuỗi Role (Kiểm tra và thêm "ROLE_" nếu chưa có)
        String normalizedCurrent = normalizeRolePrefix(currentUserRole);
        String normalizedTarget = normalizeRolePrefix(targetUserRole);

        // Lấy level của từng role từ map
        Integer currentLevel = ROLE_HIERARCHY_MAP.get(normalizedCurrent);
        Integer targetLevel = ROLE_HIERARCHY_MAP.get(normalizedTarget);

        // So sánh quyền của 2 role
        return currentLevel != null && targetLevel != null && currentLevel > targetLevel;
    }

    // Chuẩn hóa tiền tố ROLE_ cho role
    public static String normalizeRolePrefix(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "";
        }
        // Nếu đã có chữ ROLE_ ở đầu thì giữ nguyên, nếu chưa có thì nối thêm
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}