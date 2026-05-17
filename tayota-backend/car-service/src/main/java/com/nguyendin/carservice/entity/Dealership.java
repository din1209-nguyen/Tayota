package com.nguyendin.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "\"DEALERSHIP\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Dealership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @NotNull
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    String name;

    @NotNull
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    String address;

    @Column(nullable = false)
    int carQuantity;

    @Column(nullable = false)
    int accessoryQuantity;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 8)
    BigDecimal latitude;

    @NotNull
    @Column(nullable = false, precision = 11, scale = 8)
    BigDecimal longitude;

    @Size(max = 255)
    @Column(unique = true, length = 255)
    String placeId;

    @Size(max = 20)
    @Column(length = 20)
    String phone;

    @Size(max = 100)
    @Column(length = 100)
    String operatingHours;

    @Builder.Default
    @Column(nullable = false)
    boolean isActive = true;

    @CreationTimestamp
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "dealership")
    List<Car> carList;

    @OneToMany(mappedBy = "dealership", cascade = CascadeType.ALL, orphanRemoval = true)
    List<AccessoryInventory> accessoryInventoryList;
}
