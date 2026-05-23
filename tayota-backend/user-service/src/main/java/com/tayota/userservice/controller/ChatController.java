package com.tayota.userservice.controller;

import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.userservice.dto.Request.SendChatMessageRequestDTO;
import com.tayota.userservice.dto.Response.ChatMessageResponseDTO;
import com.tayota.userservice.dto.Response.ChatSessionResponseDTO;
import com.tayota.userservice.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
// Controller để xử lý các yêu cầu liên quan đến chat, bao gồm lấy phiên chat hiện tại, merge phiên chat, lấy lịch sử tin nhắn và gửi tin nhắn mới trong phiên chat. Sử dụng ChatService để thực hiện các thao tác nghiệp vụ liên quan đến chat.
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/sessions/current")
    // Endpoint để lấy hoặc tạo phiên chat hiện tại cho khách hàng. Nếu khách hàng đã có phiên chat đang hoạt động, trả về thông tin của phiên chat đó, nếu chưa có sẽ tạo mới một phiên chat và trả về thông tin của phiên chat mới tạo.
    public ApiResponse<ChatSessionResponseDTO> currentSession(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ApiResponse.success(
                200,
                "Lấy phiên chat hiện tại thành công",
                chatService.getOrCreateCurrentSessionResponse(request, response)
        );
    }

    @PostMapping("/sessions/merge")
    // Endpoint để merge phiên chat hiện tại của khách hàng với một phiên chat khác nếu có. Điều này hữu ích khi khách hàng đã có một phiên chat đang hoạt động và muốn tiếp tục cuộc trò chuyện đó thay vì tạo một phiên chat mới.
    public ApiResponse<ChatSessionResponseDTO> mergeSession(HttpServletRequest request) {
        return ApiResponse.success(
                200,
                "Merge phiên chat thành công",
                chatService.mergeCurrentSession(request)
        );
    }

    @GetMapping("/sessions/current/messages")
    // Endpoint để lấy lịch sử tin nhắn của phiên chat hiện tại. Trả về danh sách các tin nhắn đã gửi và nhận trong phiên chat, giúp khách hàng có thể xem lại nội dung cuộc trò chuyện trước đó.
    public ApiResponse<List<ChatMessageResponseDTO>> currentMessages(HttpServletRequest request) {
        return ApiResponse.success(
                200,
                "Lấy lịch sử chat thành công",
                chatService.getCurrentSessionMessages(request)
        );
    }

    @PostMapping("/messages")
    // Endpoint để gửi một tin nhắn mới trong phiên chat hiện tại. Khách hàng có thể gửi nội dung tin nhắn thông qua yêu cầu này, và tin nhắn sẽ được lưu vào cơ sở dữ liệu và trả về thông tin của tin nhắn vừa gửi.
    public ApiResponse<ChatMessageResponseDTO> sendMessage(
            @Valid @RequestBody SendChatMessageRequestDTO requestDTO,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ApiResponse.success(
                200,
                "Gửi tin nhắn thành công",
                chatService.customerSendMessage(requestDTO.getContent(), request, response)
        );
    }
}