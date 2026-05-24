package com.tayota.userservice.entity;

import com.tayota.userservice.enums.ChatSessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"CHAT_SESSION\"")
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "guest_id", length = 100)
    private String guestId;

    @Column(name = "assigned_staff_id")
    private UUID assignedAssistantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatSessionStatus status;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
