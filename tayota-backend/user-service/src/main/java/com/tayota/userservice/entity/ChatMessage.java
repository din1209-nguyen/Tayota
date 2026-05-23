package com.tayota.userservice.entity;

import com.tayota.userservice.enums.ChatSenderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"CHAT_MESSAGE\"")
// Represents a single message in a chat session
// Each message is linked to a ChatSession and contains information about the sender and content.
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;// Unique identifier for the chat message

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;// Many-to-one relationship with ChatSession, indicating which session this message belongs to

    @Column(name = "sender_id")
    private UUID senderId;// ID of the sender (could be a user or staff)

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 30)
    private ChatSenderType senderType;// Enum indicating the type of sender (CUSTOMER, STAFF, SYSTEM)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;// The actual text content of the message

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;// Timestamp indicating when the message was created
}