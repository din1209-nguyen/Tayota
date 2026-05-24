package com.tayota.operationservice.dto.Request.workorder;

import com.tayota.operationservice.enums.workorder.BillingType;
import com.tayota.operationservice.enums.workorder.ServiceItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

// Dùng để nhận dữ liệu khi thêm hạng mục dịch vụ vào phiếu dịch vụ
@Getter
public class CreateServiceItemRequest {
    // Loại hạng mục dịch vụ, ví dụ: "Phụ tùng, công thợ".
    @NotNull(message = "Loại hạng mục không được để trống")
    private ServiceItemType itemType;

    // ID của phụ tùng nếu hạng mục là phụ tùng, có thể null nếu hạng mục là công thợ.
    private UUID accessoryId;

    // Tên hạng mục dịch vụ, ví dụ: "dầu động cơ", "lọc gió", "bugi", v.v.
    @NotBlank(message = "Tên hạng mục không được để trống")
    @Size(max = 255, message = "Tên hạng mục không được vượt quá 255 ký tự")
    private String itemName;

    // Số lượng của hạng mục dịch vụ, phải lớn hơn 0.
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    // Đơn giá của hạng mục dịch vụ. Chỉ bắt buộc khi billingType là NORMAL.
    private BigDecimal unitPrice;

    // Hình thức tính phí, bình thường, bảo hành, quà tặng.
    @NotNull(message = "Hình thức tính phí không được để trống")
    private BillingType billingType;

    // Ghi chú bổ sung cho hạng mục dịch vụ, có thể null nếu không có ghi chú.
    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;
}
