package com.tayota.operationservice.controller.chat;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.request.chat.SendChatMessageRequestDTO;
import com.tayota.operationservice.dto.response.chat.ChatMessageResponseDTO;
import com.tayota.operationservice.dto.response.chat.ChatSessionResponseDTO;
import com.tayota.operationservice.enums.chat.ChatSessionStatus;
import com.tayota.operationservice.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/assistant/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('ASSISTANT')")
public class AssistantChatController {
    private final ChatService chatService;

    // Lấy danh sách phiên chat theo trạng thái
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionResponseDTO>> getSessions(
            @RequestParam(name = "status", defaultValue = "WAITING") ChatSessionStatus chatSessionStatus
    ) {
        List<ChatSessionResponseDTO> chatSessions = chatService.getAssistantSessions(chatSessionStatus);
        return ApiResponse.success(200, "Lấy danh sách phiên chat thành công", chatSessions);
    }

    // Lấy lịch sử tin nhắn của một phiên chat
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponseDTO>> getMessages(@PathVariable UUID sessionId) {
        List<ChatMessageResponseDTO> chatMessages = chatService.getAssistantSessionMessages(sessionId);
        return ApiResponse.success(200, "Lấy lịch sử chat thành công", chatMessages);
    }

    // Nhận một phiên chat để hỗ trợ
    @PatchMapping("/sessions/{sessionId}/assign")
    public ApiResponse<ChatSessionResponseDTO> assignSession(@PathVariable UUID sessionId) {
        ChatSessionResponseDTO chatSession = chatService.assignSession(sessionId);
        return ApiResponse.success(200, "Nhận phiên chat thành công", chatSession);
    }

    // Đánh dấu phiên chat đã xử lý
    @PatchMapping("/sessions/{sessionId}/resolve")
    public ApiResponse<ChatSessionResponseDTO> resolveSession(@PathVariable UUID sessionId) {
        ChatSessionResponseDTO chatSession = chatService.resolveSession(sessionId);
        return ApiResponse.success(200, "Đánh dấu phiên chat đã xử lý", chatSession);
    }

    // Đóng phiên chat
    @PatchMapping("/sessions/{sessionId}/close")
    public ApiResponse<ChatSessionResponseDTO> closeSession(@PathVariable UUID sessionId) {
        ChatSessionResponseDTO chatSession = chatService.closeSession(sessionId);
        return ApiResponse.success(200, "Đóng phiên chat thành công", chatSession);
    }

    // Gửi tin nhắn trong phiên chat
    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<ChatMessageResponseDTO> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendChatMessageRequestDTO sendChatMessageRequestDTO
    ) {
        ChatMessageResponseDTO chatMessage = chatService.assistantSendMessage(
                sessionId,
                sendChatMessageRequestDTO.getContent()
        );
        return ApiResponse.success(200, "Gửi tin nhắn thành công", chatMessage);
    }
}
