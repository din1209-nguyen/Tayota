package com.tayota.operationservice.repository.chat;

import com.tayota.operationservice.entity.chat.ChatSession;
import com.tayota.operationservice.enums.chat.ChatSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    // Tìm tất cả phiên chat theo trạng thái
    List<ChatSession> findByStatusOrderByUpdatedAtDesc(ChatSessionStatus status);

    List<ChatSession> findByUserIdAndStatusInOrderByUpdatedAtDesc(UUID userId, Collection<ChatSessionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from ChatSession session where session.id = :id")
    Optional<ChatSession> findWithLockById(@Param("id") UUID id);

    // Tìm tất cả phiên chat của một người dùng
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    // Tìm phiên chat mới nhất của một người dùng
    Optional<ChatSession> findFirstByUserIdOrderByUpdatedAtDesc(UUID userId);

    // Tìm phiên chat mới nhất của một khách vãng lai
    Optional<ChatSession> findFirstByGuestIdOrderByUpdatedAtDesc(String guestId);
}
