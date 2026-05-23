package com.tayota.userservice.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// DTO cho yêu cầu gửi tin nhắn trong một phiên chat. Chứa nội dung tin nhắn và các ràng buộc về độ dài và không được để trống.
public class SendChatMessageRequestDTO {
    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(max = 2000, message = "Nội dung tin nhắn không được vượt quá 2000 ký tự")
    private String content;
}