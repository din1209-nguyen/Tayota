package com.nguyendin.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"ACCESSORY_INVENTORY\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccessoryInventory {

    @EmbeddedId
    AccessoryInventoryId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("dealershipId")
    @JoinColumn(name = "dealership_id", nullable = false)
    Dealership dealership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("accessoryId")
    @JoinColumn(name = "accessory_id", nullable = false)
    Accessory accessory;

    @Builder.Default
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    Integer quantity = 0;

    @UpdateTimestamp
    LocalDateTime lastUpdated;
}
