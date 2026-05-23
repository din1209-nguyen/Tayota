package com.tayota.userservice.dto.Response;

import com.tayota.userservice.enums.ChatSenderType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
// Dữ liệu phản hồi cho một tin nhắn trong phiên chat, bao gồm thông tin về người gửi, nội dung và thời gian tạo.
public class ChatMessageResponseDTO {
    private UUID id; // ID của tin nhắn
    private UUID sessionId; // ID của phiên chat mà tin nhắn này thuộc về
    private UUID senderId; // ID của người gửi (có thể là user hoặc staff)
    private ChatSenderType senderType; // Loại người gửi
    private String content; // Nội dung tin nhắn
    private Instant createdAt; // Thời gian tạo tin nhắn
}