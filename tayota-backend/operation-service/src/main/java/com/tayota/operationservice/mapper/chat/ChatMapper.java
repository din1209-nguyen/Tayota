package com.tayota.operationservice.mapper.chat;

import com.tayota.operationservice.dto.response.chat.ChatMessageResponseDTO;
import com.tayota.operationservice.dto.response.chat.ChatSessionResponseDTO;
import com.tayota.operationservice.entity.chat.ChatMessage;
import com.tayota.operationservice.entity.chat.ChatSession;

public class ChatMapper {
    // Chuyển ChatMessage sang DTO phản hồi
    public static ChatMessageResponseDTO toMessageResponse(ChatMessage chatMessage) {
        return ChatMessageResponseDTO.builder()
                .id(chatMessage.getId())
                .sessionId(chatMessage.getSession().getId())
                .senderId(chatMessage.getSenderId())
                .senderType(chatMessage.getSenderType())
                .content(chatMessage.getContent())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }

    // Chuyển ChatSession sang DTO phản hồi
    public static ChatSessionResponseDTO toSessionResponse(ChatSession chatSession) {
        return ChatSessionResponseDTO.builder()
                .id(chatSession.getId())
                .userId(chatSession.getUserId())
                .guestId(chatSession.getGuestId())
                .assignedAssistantId(chatSession.getAssignedAssistantId())
                .status(chatSession.getStatus())
                .createdAt(chatSession.getCreatedAt())
                .updatedAt(chatSession.getUpdatedAt())
                .closedAt(chatSession.getClosedAt())
                .resolvedAt(chatSession.getResolvedAt())
                .build();
    }
}
