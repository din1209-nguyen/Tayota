package com.tayota.operationservice.dto.response.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// DTO này dùng để trả về số lượng thông báo chưa đọc của người dùng
public class UnreadNotificationCountResponse {
    private long unreadCount;
}
