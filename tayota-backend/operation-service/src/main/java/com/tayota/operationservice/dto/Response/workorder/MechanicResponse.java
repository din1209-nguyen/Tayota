package com.tayota.operationservice.dto.response.workorder;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MechanicResponse {
    // ID của thợ sửa xe
    private UUID id;

    // Tên đầy đủ của thợ sửa xe, được lấy từ bảng UserProfile dựa trên ID
    private String fullName;

    // Chuyên môn của thợ sửa xe, ví dụ: "Động cơ", "Hệ thống điện", "Hệ thống treo", v.v.
    private String specialty;

    // Số sao đánh giá trung bình của thợ sửa xe, ví dụ: 4.5
    private BigDecimal averageRating;
}
