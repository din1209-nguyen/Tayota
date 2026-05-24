package com.tayota.operationservice.entity.car;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "\"ACCESSORY_INVENTORY\"")
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Column(nullable = false)
    Integer quantity = 0;

    @UpdateTimestamp
    @Column(name = "last_updated")
    Instant lastUpdated;
}
