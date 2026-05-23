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
// Represents a chat session between a user (customer) and staff.
// Each session has a status (WAITING, CHATTING, RESOLVED, CLOSED) and can be associated with either a registered user (userId) or a guest (guestId).
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;// Unique identifier for the chat session

    @Column(name = "user_id")
    private UUID userId;// ID of the registered user participating in the chat session (nullable if it's a guest session)

    @Column(name = "guest_id", length = 100)
    private String guestId;// Identifier for guest users (nullable if it's a registered user session)

    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;// ID of the staff member assigned to this chat session (nullable if not yet assigned)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatSessionStatus status;// Enum indicating the current status of the chat session (WAITING, CHATTING, RESOLVED, CLOSED)

    @Column(name = "closed_at")
    private Instant closedAt;// Timestamp indicating when the chat session was closed (nullable if not yet closed)

    @Column(name = "resolved_at")
    private Instant resolvedAt;// Timestamp indicating when the chat session was resolved (nullable if not yet resolved)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;// Timestamp indicating when the chat session was created

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;// Timestamp indicating when the chat session was last updated
}