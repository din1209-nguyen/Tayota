package com.tayota.operationservice.util;

import com.tayota.operationservice.dto.response.auth.DeviceResponseDTO;
import com.tayota.operationservice.object.auth.UserSession;
import com.tayota.operationservice.service.cache.SystemCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionUtil {
    private final SystemCacheService systemCacheService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpirationMs;

    private static final String REFRESH_TOKEN_KEY = "auth:refresh:";
    private static final String USER_SESSIONS_KEY = "auth:user_sessions:";

    // Lấy danh sách các thiết bị người dùng
    public List<DeviceResponseDTO> getUserDevices(String userId) {
        List<DeviceResponseDTO> deviceList = new ArrayList<>();

        // Lấy danh sách các thiết bị
        String userSessionsKey = USER_SESSIONS_KEY + userId;
        Set<Object> devices = systemCacheService.setMembers(userSessionsKey);

        if (devices != null) {
            // Duyệt từng thiết bị trong danh sách user_sessions
            for (Object d : devices) {
                if (d == null) continue;
                String deviceId = d.toString();
                String sessionKey = REFRESH_TOKEN_KEY + userId + ":" + deviceId;

                // Lấy session người dùng từ cache hệ thống.
                Object sessionData = systemCacheService.get(sessionKey);

                if (sessionData != null) {
                    // Chuyển đổi thành UserSession object
                    UserSession userSession = objectMapper.convertValue(sessionData, UserSession.class);

                    // Thêm vào danh sách thiết bị trả về
                    DeviceResponseDTO deviceDTO = DeviceResponseDTO.builder()
                            .deviceId(deviceId)
                            .clientIp(userSession.getClientIp())
                            .userAgent(userSession.getUserAgent())
                            .loginAt(userSession.getLoginAt())
                            .build();

                    deviceList.add(deviceDTO);
                }
            }
        }

        // Trả về danh sách các thiết bị
        return deviceList;
    }

    // Lưu session của thiết bị người dùng
    public void saveSession(String userId, String deviceId, String refreshToken, String clientIp, String userAgent) {
        // Tạo session key
        String sessionKey = REFRESH_TOKEN_KEY + userId + ":" + deviceId;

        // Băm refresh token trước khi lưu
        String refreshHash = jwtUtil.hashRefreshToken(refreshToken);

        // Tạo session value
        UserSession sessionValue = new UserSession(refreshHash, clientIp, userAgent, Instant.now());

        // Lưu session vào cache hệ thống và đặt thời gian sống bằng thời gian refresh-token.
        systemCacheService.put(sessionKey, sessionValue, Duration.ofMillis(jwtRefreshTokenExpirationMs));

        // Thêm deviceId vào danh sách user_sessions và đặt thời gian sống bằng refresh-token
        String userSessionsKey = USER_SESSIONS_KEY + userId;
        systemCacheService.setAdd(userSessionsKey, deviceId);
        systemCacheService.expire(userSessionsKey, Duration.ofMillis(jwtRefreshTokenExpirationMs));
    }

    // Xóa session của thiết bị người dùng
    public void deleteSession(String userId, String deviceId) {
        // Xoá session của deviceId
        String sessionKey = REFRESH_TOKEN_KEY + userId + ":" + deviceId;
        systemCacheService.delete(sessionKey);

        // Xoá deviceId khỏi danh sách user_sessions
        String userSessionsKey = USER_SESSIONS_KEY + userId;
        systemCacheService.setRemove(userSessionsKey, deviceId);
    }

    // Xóa tất cả session của thiết bị người dùng
    public void deleteAllSessions(String userId) {
        /*
        * Không nên duyệt toàn bộ cache hệ thống để tìm và xoá userId sẽ tốn kém.
        * Nên ta tạo danh sách thiết bị (user_sessions) để dễ truy xuất từng deviceId và xoá
        */

        // Tạo key danh sách deviceId của người dùng
        String userSessionsKey = USER_SESSIONS_KEY + userId;

        // Lấy danh sách các thiết bị người dùng
        Set<Object> devices = systemCacheService.setMembers(userSessionsKey);

        // Duyệt từng thiết bị trong danh sách user_sessions
        if (devices != null) {
            for (Object d : devices) {
                if (d == null) continue;
                String deviceId = d.toString();
                String sessionKey = REFRESH_TOKEN_KEY + userId + ":" + deviceId;

                // Xóa session của từng deviceId
                systemCacheService.delete(sessionKey);
            }
        }

        // Xóa danh sách deviceId của người dùng
        systemCacheService.delete(userSessionsKey);
    }

    public int countActiveDevices(String userId) {
        // Tạo key danh sách deviceId của người dùng
        String userSessionsKey = USER_SESSIONS_KEY + userId;

        // Lấy danh sách các thiết bị người dùng
        Set<Object> devices = systemCacheService.setMembers(userSessionsKey);

        // Nếu chưa tồn tại thiết bị nào đăng nhập
        if (devices == null) return 0;

        // Biến đếm số lượng thiết bị đang hoạt động
        int activeCount = 0;

        // Duyệt từng thiết bị trong danh sách user_sessions
        for (Object d : devices) {
            if (d == null) continue;
            String deviceId = d.toString();
            String sessionKey = REFRESH_TOKEN_KEY + userId + ":" + deviceId;

            // Kiểm tra nếu session của thiết bị này còn tồn tại trong cache hệ thống.
            if (systemCacheService.hasKey(sessionKey)) {
                activeCount++;
            }
            else {
                // Nếu session đã hết hạn hoặc bị xoá, xoá deviceId khỏi user_sessions để sạch dữ liệu không bị lỗi
                systemCacheService.setRemove(userSessionsKey, deviceId);
            }
        }
        return activeCount;
    }
}



