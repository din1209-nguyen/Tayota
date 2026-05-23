package com.tayota.userservice.controller;

import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.userservice.dto.Request.SendChatMessageRequestDTO;
import com.tayota.userservice.dto.Response.ChatMessageResponseDTO;
import com.tayota.userservice.dto.Response.ChatSessionResponseDTO;
import com.tayota.userservice.enums.ChatSessionStatus;
import com.tayota.userservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/staff/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('ASSISTANT')")
// Controller để xử lý các yêu cầu liên quan đến chat dành cho nhân viên hỗ trợ khách hàng, bao gồm lấy danh sách phiên chat theo trạng thái, lấy lịch sử tin nhắn của một phiên chat cụ thể, nhận một phiên chat để hỗ trợ, đánh dấu phiên chat đã xử lý, đóng phiên chat và gửi tin nhắn mới trong phiên chat. 
//Sử dụng ChatService để thực hiện các thao tác nghiệp vụ liên quan đến chat.
public class StaffChatController {
    private final ChatService chatService;

    @GetMapping("/sessions")
    // Endpoint để lấy danh sách các phiên chat theo trạng thái. Nhân viên có thể lọc các phiên chat dựa trên trạng thái như WAITING, ASSIGNED, RESOLVED hoặc CLOSED để quản lý và hỗ trợ khách hàng hiệu quả hơn.
    public ApiResponse<List<ChatSessionResponseDTO>> getSessions(
            @RequestParam(defaultValue = "WAITING") ChatSessionStatus status
    ) {
        return ApiResponse.success(200, "Lấy danh sách phiên chat thành công", chatService.getStaffSessions(status));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    // Endpoint để lấy lịch sử tin nhắn của một phiên chat cụ thể. Nhân viên có thể xem lại nội dung cuộc trò chuyện trong một phiên chat nhất định.
    public ApiResponse<List<ChatMessageResponseDTO>> getMessages(@PathVariable UUID sessionId) {
        return ApiResponse.success(200, "Lấy lịch sử chat thành công", chatService.getStaffSessionMessages(sessionId));
    }

    @PatchMapping("/sessions/{sessionId}/assign")
    // Endpoint để nhận một phiên chat để hỗ trợ. Nhân viên có thể sử dụng endpoint này để lấy quyền quản lý một phiên chat đang chờ xử lý.
    public ApiResponse<ChatSessionResponseDTO> assign(@PathVariable UUID sessionId) {
        return ApiResponse.success(200, "Nhận phiên chat thành công", chatService.assignSession(sessionId));
    }

    @PatchMapping("/sessions/{sessionId}/resolve")
    // Endpoint để đánh dấu một phiên chat đã được xử lý. Nhân viên có thể sử dụng endpoint này để cập nhật trạng thái của phiên chat.
    public ApiResponse<ChatSessionResponseDTO> resolve(@PathVariable UUID sessionId) {
        return ApiResponse.success(200, "Đánh dấu phiên chat đã xử lý", chatService.resolveSession(sessionId));
    }

    @PatchMapping("/sessions/{sessionId}/close")
    // Endpoint để đóng một phiên chat. Nhân viên có thể sử dụng endpoint này để đóng một phiên chat đang hoạt động.
    public ApiResponse<ChatSessionResponseDTO> close(@PathVariable UUID sessionId) {
        return ApiResponse.success(200, "Đóng phiên chat thành công", chatService.closeSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    // Endpoint để gửi một tin nhắn mới trong phiên chat hiện tại. Nhân viên có thể gửi nội dung tin nhắn thông qua yêu cầu này, và tin nhắn sẽ được lưu vào cơ sở dữ liệu và trả về thông tin của tin nhắn vừa gửi.
    public ApiResponse<ChatMessageResponseDTO> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendChatMessageRequestDTO requestDTO
    ) {
        return ApiResponse.success(
                200,
                "Gửi tin nhắn thành công",
                chatService.staffSendMessage(sessionId, requestDTO.getContent())
        );
    }
}