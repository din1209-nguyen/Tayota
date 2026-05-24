package com.tayota.operationservice.controller;

import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.operationservice.dto.Request.SendChatMessageRequestDTO;
import com.tayota.operationservice.dto.Response.ChatMessageResponseDTO;
import com.tayota.operationservice.dto.Response.ChatSessionResponseDTO;
import com.tayota.operationservice.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    // Lấy hoặc tạo phiên chat hiện tại
    @PostMapping("/sessions/current")
    public ApiResponse<ChatSessionResponseDTO> getCurrentSession(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ChatSessionResponseDTO chatSession = chatService.getOrCreateCurrentSessionResponse(request, response);
        return ApiResponse.success(200, "Lấy phiên chat hiện tại thành công", chatSession);
    }

    // Gộp phiên chat hiện tại
    @PostMapping("/sessions/merge")
    public ApiResponse<ChatSessionResponseDTO> mergeSession(HttpServletRequest request) {
        ChatSessionResponseDTO chatSession = chatService.mergeCurrentSession(request);
        return ApiResponse.success(200, "Merge phiên chat thành công", chatSession);
    }

    // Lấy lịch sử tin nhắn của phiên chat hiện tại
    @GetMapping("/sessions/current/messages")
    public ApiResponse<List<ChatMessageResponseDTO>> getCurrentMessages(HttpServletRequest request) {
        List<ChatMessageResponseDTO> chatMessages = chatService.getCurrentSessionMessages(request);
        return ApiResponse.success(200, "Lấy lịch sử chat thành công", chatMessages);
    }

    // Gửi tin nhắn trong phiên chat hiện tại
    @PostMapping("/messages")
    public ApiResponse<ChatMessageResponseDTO> sendMessage(
            @Valid @RequestBody SendChatMessageRequestDTO sendChatMessageRequestDTO,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ChatMessageResponseDTO chatMessage = chatService.customerSendMessage(
                sendChatMessageRequestDTO.getContent(),
                request,
                response
        );
        return ApiResponse.success(200, "Gửi tin nhắn thành công", chatMessage);
    }
}
