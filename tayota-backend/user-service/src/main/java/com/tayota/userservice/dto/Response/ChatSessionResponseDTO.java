package com.tayota.userservice.dto.Response;

import com.tayota.userservice.enums.ChatSessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
// Dữ liệu phản hồi cho một phiên chat, bao gồm thông tin về người dùng, trạng thái và thời gian tạo.
public class ChatSessionResponseDTO {
    private UUID id;// ID của phiên chat
    private UUID userId;// ID của người dùng tham gia phiên chat
    private String guestId;// ID của khách tham gia phiên chat (nếu có)
    private UUID assignedStaffId;// ID của nhân viên được chỉ định để hỗ trợ phiên chat (nếu có)
    private ChatSessionStatus status;// Trạng thái hiện tại của phiên chat
    private Instant createdAt;// Thời gian tạo phiên chat
    private Instant updatedAt;// Thời gian cập nhật phiên chat gần nhất
    private Instant closedAt;// Thời gian đóng phiên chat (nếu đã đóng)
    private Instant resolvedAt;// Thời gian giải quyết phiên chat (nếu đã giải quyết)
}