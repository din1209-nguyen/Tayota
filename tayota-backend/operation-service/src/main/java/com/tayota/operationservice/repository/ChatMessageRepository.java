package com.tayota.operationservice.repository;

import com.tayota.operationservice.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    // Tìm tất cả tin nhắn của một phiên chat
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
