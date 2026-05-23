package com.tayota.userservice.repository;

import com.tayota.userservice.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Repository để quản lý ChatMessage, hỗ trợ truy vấn theo sessionId và sắp xếp theo thời gian tạo
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    // Tìm tất cả ChatMessage theo sessionId, sắp xếp theo thời gian tạo tăng dần (cũ nhất trước)
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}