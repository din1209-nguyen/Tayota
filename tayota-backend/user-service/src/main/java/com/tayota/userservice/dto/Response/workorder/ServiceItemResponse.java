package com.tayota.userservice.dto.Response.workorder;

import com.tayota.userservice.enums.workorder.BillingType;
import com.tayota.userservice.enums.workorder.ServiceItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

// DTO để trả về thông tin chi tiết của một hạng mục dịch vụ trong phiếu dịch vụ
@Getter
@AllArgsConstructor
public class ServiceItemResponse {
    // ID của hạng mục dịch vụ
    private UUID id;

    // Loại hạng mục dịch vụ.
    private ServiceItemType itemType;

    // ID của phụ tùng tương ứng, có thể null
    private UUID accessoryId;

    // Tên của dịch vụ hoặc phụ tùng, được lấy từ bảng Accessory
    private String itemName;

    // Số lượng của hạng mục dịch vụ, ví dụ: 1, 2, v.v.
    private Integer quantity;

    // Đơn giá của hạng mục dịch vụ, ví dụ: 100.00
    private BigDecimal unitPrice;

    // Hình thức tính phí của hạng mục dịch vụ, ví dụ: NORMAL, WARRANTY, GIFT
    private BillingType billingType;

    // Tổng tiền của hạng mục dịch vụ, được tính bằng quantity * unitPrice, ví dụ: 200.00
    private BigDecimal finalPrice;

    // Ghi chú bổ sung cho hạng mục dịch vụ, có thể null nếu không có ghi chú.
    private String note;
}
