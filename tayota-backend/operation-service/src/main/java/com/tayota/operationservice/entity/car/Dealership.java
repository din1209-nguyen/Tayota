package com.tayota.operationservice.entity.car;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "\"DEALERSHIP\"")
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Dealership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    UUID id;

    @Column(nullable = false, length = 150)
    String name;

    @Column(nullable = false, length = 255)
    String address;

    @Column(name = "car_quantity")
    @Builder.Default
    Integer carQuantity = 0;

    @Column(nullable = false, precision = 10, scale = 8)
    BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    BigDecimal longitude;

    @Column(name = "place_id", unique = true, length = 255)
    String placeId;

    @Column(length = 20)
    String phone;

    @Column(name = "operating_hours", length = 100)
    String operatingHours;

    @Builder.Default
    @Column(name = "is_active")
    boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    Instant createdAt;

    @OneToMany(mappedBy = "dealership")
    List<Car> carList;

}
