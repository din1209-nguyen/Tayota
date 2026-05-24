package com.tayota.userservice.controller;

import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.dto.Response.notification.NotificationResponse;
import com.tayota.userservice.dto.Response.notification.UnreadNotificationCountResponse;
import com.tayota.userservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    // Lấy danh sách thông báo của user đang đăng nhập.
    @GetMapping("/my")
    public ApiResponse<List<NotificationResponse>> getMyNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = parseRequiredUserId(userIdHeader);
        List<NotificationResponse> response = notificationService.getMyNotifications(userId);

        return ApiResponse.success(200, "Lấy danh sách thông báo thành công!", response);
    }

    // Lấy số lượng thông báo chưa đọc để FE hiển thị badge.
    @GetMapping("/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> getMyUnreadCount(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = parseRequiredUserId(userIdHeader);
        UnreadNotificationCountResponse response = notificationService.getMyUnreadCount(userId);

        return ApiResponse.success(200, "Lấy số thông báo chưa đọc thành công!", response);
    }

    // Đánh dấu một thông báo là đã đọc.
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable UUID notificationId
    ) {
        UUID userId = parseRequiredUserId(userIdHeader);
        NotificationResponse response = notificationService.markAsRead(notificationId, userId);

        return ApiResponse.success(200, "Đánh dấu thông báo đã đọc thành công!", response);
    }

    // Đánh dấu toàn bộ thông báo là đã đọc.
    @PatchMapping("/read-all")
    public ApiResponse<UnreadNotificationCountResponse> markAllAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = parseRequiredUserId(userIdHeader);
        UnreadNotificationCountResponse response = notificationService.markAllAsRead(userId);

        return ApiResponse.success(200, "Đánh dấu tất cả thông báo đã đọc thành công!", response);
    }

    // Phương thức tiện ích để lấy và xác thực userId từ header, đảm bảo rằng user đã đăng nhập và thông tin hợp lệ.
    private UUID parseRequiredUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new CustomException(401, "Vui lòng đăng nhập để xem thông báo");
        }

        try {
            return UUID.fromString(userIdHeader.trim());
        } catch (IllegalArgumentException exception) {
            throw new CustomException(401, "Thông tin người dùng không hợp lệ");
        }
    }
}
