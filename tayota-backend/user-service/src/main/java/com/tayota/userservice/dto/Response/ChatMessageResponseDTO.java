package com.tayota.userservice.dto.Response;

import com.tayota.userservice.enums.ChatSenderType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChatMessageResponseDTO {
    // Trả về ID tin nhắn
    private UUID id;

    // Trả về ID phiên chat
    private UUID sessionId;

    // Trả về ID người gửi
    private UUID senderId;

    // Trả về loại người gửi
    private ChatSenderType senderType;

    // Trả về nội dung tin nhắn
    private String content;

    // Trả về thời điểm tạo tin nhắn
    private Instant createdAt;
}
