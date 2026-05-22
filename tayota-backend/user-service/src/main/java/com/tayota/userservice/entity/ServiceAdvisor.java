package com.tayota.userservice.entity;


import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

// Bảng này lưu thông tin về Service Advisor, mỗi Service Advisor sẽ thuộc về một đại lý cụ thể (dealershipId)
@Entity
@Table(name = "\"SERVICE_ADVISOR\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceAdvisor {
    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(name = "dealership_id", nullable = false)
    private UUID dealershipId;

}
