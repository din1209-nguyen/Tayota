package com.tayota.userservice.repository;

import com.tayota.userservice.entity.ChatSession;
import com.tayota.userservice.enums.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    // Tìm tất cả phiên chat theo trạng thái
    List<ChatSession> findByStatusOrderByUpdatedAtDesc(ChatSessionStatus status);

    // Tìm tất cả phiên chat của một người dùng
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    // Tìm phiên chat mới nhất của một người dùng
    Optional<ChatSession> findFirstByUserIdOrderByUpdatedAtDesc(UUID userId);

    // Tìm phiên chat mới nhất của một khách vãng lai
    Optional<ChatSession> findFirstByGuestIdOrderByUpdatedAtDesc(String guestId);
}
