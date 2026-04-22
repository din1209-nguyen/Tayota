package com.tayota.userservice.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public class IpUtil {

    // Danh sách IP của proxy đáng tin cậy (ví dụ: Nginx chạy cùng server)
    // Chỉ khi request đến từ các IP này thì mới đọc header X-Forwarded-For
    // Điều này giúp chống giả mạo header từ client
    private static final List<String> TRUSTED_PROXIES = List.of(
            "127.0.0.1", // Nginx chạy local cùng server (IPv4)
            "::1"        // Localhost IPv6
    );

    // Lấy IP thật của client từ HttpServletRequest
    public static String getClientIp(HttpServletRequest request) {

        // Lấy IP kết nối trực tiếp tới server
        // - Nếu không có proxy => đây là IP client
        // - Nếu có proxy => đây là IP của proxy (ví dụ: Nginx)
        String remoteAddr = request.getRemoteAddr();

        // Chuẩn hóa IPv6 localhost về IPv4 localhost
        // Giúp so sánh dễ dàng với TRUSTED_PROXIES
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            remoteAddr = "127.0.0.1";
        }

        // Nếu request không đến từ proxy đáng tin cậy
        // => bỏ qua header X-Forwarded-For
        // => trả về remoteAddr (IP thật của client)
        // Giúp chống giả mạo header
        if (!TRUSTED_PROXIES.contains(remoteAddr)) {
            return remoteAddr;
        }

        // Đọc header X-Forwarded-For (do proxy thêm vào)
        // Header này chứa IP thật của client
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        // Nếu header không tồn tại hoặc không hợp lệ => fallback về remoteAddr
        if (xForwardedFor == null || xForwardedFor.isBlank() || "unknown".equalsIgnoreCase(xForwardedFor)) {
            return remoteAddr;
        }

        // Nếu có nhiều IP:
        // Ví dụ:
        // X-Forwarded-For: clientIP, proxy1, proxy2
        // Thì chỉ lấy IP đầu tiên (IP thật của client)
        return xForwardedFor.split(",")[0].trim();
    }
}