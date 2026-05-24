package com.tayota.operationservice.websocket;

import com.tayota.operationservice.dto.Request.SendChatMessageRequestDTO;
import com.tayota.operationservice.dto.Response.ChatMessageResponseDTO;
import com.tayota.operationservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    private final ChatService chatService;

    // Gửi tin nhắn khách hàng qua WebSocket
    @MessageMapping("/chat.send")
    public ChatMessageResponseDTO sendCustomerMessage(
            @Valid @Payload SendChatMessageRequestDTO sendChatMessageRequestDTO,
            @Header("chat_session") String chatSession
    ) {
        UUID chatSessionId = UUID.fromString(chatSession);
        return chatService.customerSendMessageToSession(chatSessionId, sendChatMessageRequestDTO.getContent());
    }

    // Gửi tin nhắn assistant qua WebSocket
    @MessageMapping("/assistant.chat.send")
    public ChatMessageResponseDTO sendAssistantMessage(
            @Valid @Payload SendChatMessageRequestDTO sendChatMessageRequestDTO,
            @Header("chat_session") String chatSession
    ) {
        UUID chatSessionId = UUID.fromString(chatSession);
        return chatService.assistantSendMessage(chatSessionId, sendChatMessageRequestDTO.getContent());
    }
}
