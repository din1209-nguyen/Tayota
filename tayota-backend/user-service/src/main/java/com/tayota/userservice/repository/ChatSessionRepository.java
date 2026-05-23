package com.tayota.userservice.repository;

import com.tayota.userservice.entity.ChatSession;
import com.tayota.userservice.enums.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repository để quản lý ChatSession, hỗ trợ các truy vấn theo trạng thái, userId và guestId
// Các phương thức truy vấn được định nghĩa theo quy ước của Spring Data JPA, giúp giảm boilerplate code
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    // Tìm tất cả ChatSession theo trạng thái, sắp xếp theo thời gian cập nhật giảm dần (mới nhất trước)
    List<ChatSession> findByStatusOrderByUpdatedAtDesc(ChatSessionStatus status);
    // Tìm tất cả ChatSession của một user theo userId, sắp xếp theo thời gian cập nhật giảm dần
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    // Tìm ChatSession mới nhất của một user theo userId, sắp xếp theo thời gian cập nhật giảm dần và lấy cái đầu tiên (mới nhất)
    Optional<ChatSession> findFirstByUserIdOrderByUpdatedAtDesc(UUID userId);
    // Tìm ChatSession mới nhất của một guest theo guestId, sắp xếp theo thời gian cập nhật giảm dần và lấy cái đầu tiên (mới nhất)
    Optional<ChatSession> findFirstByGuestIdOrderByUpdatedAtDesc(String guestId);
}