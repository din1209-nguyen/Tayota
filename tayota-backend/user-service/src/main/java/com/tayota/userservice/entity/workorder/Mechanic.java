package com.tayota.userservice.entity.workorder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"MECHANIC\"")
public class Mechanic {
    // ID của thợ sửa xe
    @Id
    @Column(updatable = false)
    private UUID id;

    // Đại lý mà thợ sửa xe này làm việc
    @Column(name = "dealership_id", nullable = false)
    private UUID dealershipId;

    // Chuyên môn của thợ sửa xe, ví dụ: "Động cơ", "Hệ thống điện", "Hệ thống treo", v.v.
    @Column(length = 100)
    private String specialty;

    // Số sao đánh giá trung bình của thợ sửa xe, ví dụ: 4.5
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    // Có đang hoạt động hay không
    @Column(name = "is_active")
    private Boolean active;
}
