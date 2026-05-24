package com.tayota.operationservice.controller.notification;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.response.notification.NotificationResponse;
import com.tayota.operationservice.dto.response.notification.UnreadNotificationCountResponse;
import com.tayota.operationservice.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    // Lấy danh sách thông báo của user đang đăng nhập
    @GetMapping("/my")
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        List<NotificationResponse> response = notificationService.getMyNotifications();

        return ApiResponse.success(200, "Lấy danh sách thông báo thành công!", response);
    }

    // Lấy số lượng thông báo chưa đọc để FE hiển thị badge
    @GetMapping("/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> getMyUnreadCount() {
        UnreadNotificationCountResponse response = notificationService.getMyUnreadCount();

        return ApiResponse.success(200, "Lấy số thông báo chưa đọc thành công!", response);
    }

    // Đánh dấu một thông báo là đã đọc
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @PathVariable UUID notificationId
    ) {
        NotificationResponse response = notificationService.markAsRead(notificationId);

        return ApiResponse.success(200, "Đánh dấu thông báo đã đọc thành công!", response);
    }

    // Đánh dấu toàn bộ thông báo là đã đọc
    @PatchMapping("/read-all")
    public ApiResponse<UnreadNotificationCountResponse> markAllAsRead() {
        UnreadNotificationCountResponse response = notificationService.markAllAsRead();

        return ApiResponse.success(200, "Đánh dấu tất cả thông báo đã đọc thành công!", response);
    }
}
