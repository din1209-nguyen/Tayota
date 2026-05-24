package com.tayota.userservice.dto.Response.notification;

import com.tayota.userservice.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

// Gửi thông tin chi tiết của một thông báo đến client, bao gồm các trường như id, loại, tiêu đề, nội dung, trạng thái đọc, thời gian đọc và thời gian tạo
@Getter
@AllArgsConstructor
public class NotificationResponse {
    // ID của thông báo, dùng để phân biệt và truy xuất thông báo
    private UUID id;

    // Loại thông báo
    private NotificationType type;

    // Tiêu đề của thông báo, có thể dùng để hiển thị trong giao diện người dùng
    private String title;

    // Nội dung chi tiết của thông báo, có thể chứa thông tin quan trọng mà người dùng cần biết
    private String content;

    // Trạng thái đọc của thông báo, dùng để xác định xem người dùng đã đọc thông báo hay chưa
    private Boolean read;

    // Thời gian mà người dùng đọc thông báo.
    private Instant createdAt;
}
