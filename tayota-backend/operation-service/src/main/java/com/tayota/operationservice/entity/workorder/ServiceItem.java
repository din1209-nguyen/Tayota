package com.tayota.operationservice.entity.workorder;


import com.tayota.operationservice.enums.workorder.BillingType;
import com.tayota.operationservice.enums.workorder.ServiceItemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"SERVICE_ITEM\"")
public class ServiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceTicket serviceTicket;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ServiceItemType itemType; // Loại hạng mục: LABOR (công thợ) hoặc PART (phụ tùng)

    @Column(name = "accessory_id")
    private UUID accessoryId;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName; // Tên hạng mục công việc hoặc phụ tùng.

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice; // Giá đơn vị của hạng mục, có thể là giá công thợ hoặc giá phụ tùng.

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", nullable = false, length = 30)
    private BillingType billingType; // Hình thức tính phí: NORMAL, WARRANTY, GIFT, nếu là WARRANTY và GIFT thì giá cuối cùng sẽ bắt buộc phải bằng 0

    @Column(name = "final_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalPrice;

    @Column(length = 500)
    private String note; // Ghi chú thêm về hạng mục, để lại thông tin quan trọng về hạng mục đã thực hiện.

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}