package com.tayota.operationservice.repository;

import com.tayota.operationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    // Lấy tất cả notification của user đang đăng nhập
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Lấy một notification thuộc user cụ thể để tránh user thao tác lên thông báo của người khác
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    // Đếm số notification chưa đọc để FE hiển thị badge thông báo
    long countByUserIdAndReadFalse(UUID userId);

    // Lấy tất cả notification chưa đọc của user để đánh dấu đã đọc hàng loạt
    List<Notification> findByUserIdAndReadFalse(UUID userId);
}
