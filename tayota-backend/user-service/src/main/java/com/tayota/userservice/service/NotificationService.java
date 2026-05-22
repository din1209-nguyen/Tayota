package com.tayota.userservice.service;

import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.dto.Response.notification.NotificationResponse;
import com.tayota.userservice.dto.Response.notification.UnreadNotificationCountResponse;
import com.tayota.userservice.entity.Notification;
import com.tayota.userservice.enums.NotificationType;
import com.tayota.userservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    // Lấy danh sách thông báo của user đang đăng nhập, chỉ trả các trường FE cần hiển thị.
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Đếm số thông báo chưa đọc của user để FE hiển thị badge.
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getMyUnreadCount(UUID userId) {
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);

        return new UnreadNotificationCountResponse(unreadCount);
    }

    // Tạo thông báo do một sender cụ thể gửi đến user (ví dụ: Service Advisor gửi thông báo về cuộc hẹn)
    @Transactional
    public Notification createNotification(
            UUID userId,
            UUID senderId,
            NotificationType type,
            String title,
            String content
    ) {
        if (userId == null) {
            return null;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .senderId(senderId)
                .type(type)
                .title(title)
                .content(content)
                .read(false)
                .build();

        return notificationRepository.save(notification);
    }

    // Đánh dấu một thông báo là đã đọc, chỉ áp dụng với thông báo thuộc user hiện tại.
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy thông báo"));

        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    // Đánh dấu toàn bộ thông báo chưa đọc của user hiện tại là đã đọc.
    @Transactional
    public UnreadNotificationCountResponse markAllAsRead(UUID userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadFalse(userId);
        Instant readAt = Instant.now();

        unreadNotifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(readAt);
        });

        notificationRepository.saveAll(unreadNotifications);

        return new UnreadNotificationCountResponse(0);
    }

    // Chuyển đổi entity Notification thành DTO NotificationResponse để trả về cho FE, chỉ chứa các trường cần thiết.
    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRead(),
                notification.getCreatedAt()
        );
    }
}
