package com.tayota.userservice.dto.Response;

import com.tayota.userservice.enums.ChatSessionStatus;
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

    // Trả về thời điểm tạo phiên chat
    private Instant createdAt;

    // Trả về thời điểm cập nhật phiên chat
    private Instant updatedAt;

    // Trả về thời điểm đóng phiên chat
    private Instant closedAt;

    // Trả về thời điểm xử lý phiên chat
    private Instant resolvedAt;
}
