package com.tayota.operationservice.dto.response.chat;

import com.tayota.operationservice.enums.chat.ChatSessionStatus;
import com.tayota.operationservice.enums.chat.ChatSenderType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChatSessionResponseDTO {
    // Trả về ID phiên chat
    private UUID id;

    // Trả về ID người dùng đã đăng nhập
    private UUID userId;

    // Trả về ID khách vãng lai
    private String guestId;

    // Trả về ID assistant phụ trách
    private UUID assignedAssistantId;

    // Trả về trạng thái phiên chat
    private ChatSessionStatus status;

    // Tên hiển thị của khách hàng trong danh sách phiên chat
    private String customerDisplayName;

    // Nội dung tin nhắn mới nhất của phiên chat
    private String lastMessageContent;

    // Loại người gửi tin nhắn mới nhất
    private ChatSenderType lastMessageSenderType;

    // Thời điểm tin nhắn mới nhất
    private Instant lastMessageAt;

    // Trả về thời điểm tạo phiên chat
    private Instant createdAt;

    // Trả về thời điểm cập nhật phiên chat
    private Instant updatedAt;

    // Trả về thời điểm đóng phiên chat
    private Instant closedAt;

    // Trả về thời điểm xử lý phiên chat
    private Instant resolvedAt;
}
