package com.tayota.userservice.websocket;

import com.tayota.userservice.dto.Request.SendChatMessageRequestDTO;
import com.tayota.userservice.dto.Response.ChatMessageResponseDTO;
import com.tayota.userservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
// Controller để xử lý các tin nhắn gửi qua WebSocket cho chức năng chat.
public class ChatSocketController {// Sử dụng ChatService để thực hiện các thao tác nghiệp vụ liên quan đến chat khi nhận được tin nhắn từ khách hàng hoặc nhân viên hỗ trợ qua WebSocket.
    private final ChatService chatService;// Phương thức để xử lý tin nhắn gửi từ khách hàng qua WebSocket. Nhận nội dung tin nhắn và sessionId từ header, sau đó gọi ChatService để lưu tin nhắn và trả về thông tin của tin nhắn vừa gửi.

    @MessageMapping("/chat.send")
    // Phương thức để xử lý tin nhắn gửi từ khách hàng qua WebSocket. 
    // Nhận nội dung tin nhắn và sessionId từ header, sau đó gọi ChatService để lưu tin nhắn và trả về thông tin của tin nhắn vừa gửi.
    public ChatMessageResponseDTO customerSend(
            @Payload SendChatMessageRequestDTO request,
            @Header("sessionId") String sessionId
    ) {
        return chatService.customerSendMessageBySessionId(UUID.fromString(sessionId), request.getContent());
    }

    @MessageMapping("/staff.chat.send")
    // Phương thức để xử lý tin nhắn gửi từ nhân viên hỗ trợ qua WebSocket.
    public ChatMessageResponseDTO staffSend(
            @Payload SendChatMessageRequestDTO request,
            @Header("sessionId") String sessionId
    ) {
        return chatService.staffSendMessage(UUID.fromString(sessionId), request.getContent());
    }
}