package com.tayota.operationservice.mapper.chat;

import com.tayota.operationservice.dto.response.chat.ChatMessageResponseDTO;
import com.tayota.operationservice.dto.response.chat.ChatSessionResponseDTO;
import com.tayota.operationservice.entity.chat.ChatMessage;
import com.tayota.operationservice.entity.chat.ChatSession;
import com.tayota.operationservice.enums.chat.ChatSenderType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChatMapper {
    // Tạo entity tin nhắn mới từ phiên chat và thông tin người gửi.
    public ChatMessage toMessageEntity(
            ChatSession chatSession,
            UUID senderId,
            ChatSenderType senderType,
            String content
    ) {
        // Gắn tin nhắn mới vào phiên chat hiện tại.
        return ChatMessage.builder()
                .session(chatSession)
                .senderId(senderId)
                .senderType(senderType)
                .content(content.trim())
                .build();
    }

    // Chuyển ChatMessage sang DTO phản hồi.
    public ChatMessageResponseDTO toMessageResponse(ChatMessage chatMessage) {
        // Trả về dữ liệu tin nhắn ở định dạng client cần hiển thị.
        return ChatMessageResponseDTO.builder()
                .id(chatMessage.getId())
                .sessionId(chatMessage.getSession().getId())
                .senderId(chatMessage.getSenderId())
                .senderType(chatMessage.getSenderType())
                .content(chatMessage.getContent())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }

    // Chuyển ChatSession sang DTO phản hồi kèm thông tin tin nhắn mới nhất.
    public ChatSessionResponseDTO toSessionResponse(
            ChatSession chatSession,
            ChatMessage lastMessage,
            String customerDisplayName
    ) {
        // Trả về dữ liệu phiên chat đã gom đủ thông tin cho widget và workspace assistant.
        return ChatSessionResponseDTO.builder()
                .id(chatSession.getId())
                .userId(chatSession.getUserId())
                .guestId(chatSession.getGuestId())
                .assignedAssistantId(chatSession.getAssignedAssistantId())
                .status(chatSession.getStatus())
                .customerDisplayName(customerDisplayName)
                .lastMessageContent(lastMessage == null ? null : lastMessage.getContent())
                .lastMessageSenderType(lastMessage == null ? null : lastMessage.getSenderType())
                .lastMessageAt(lastMessage == null ? null : lastMessage.getCreatedAt())
                .createdAt(chatSession.getCreatedAt())
                .updatedAt(chatSession.getUpdatedAt())
                .closedAt(chatSession.getClosedAt())
                .build();
    }
}
