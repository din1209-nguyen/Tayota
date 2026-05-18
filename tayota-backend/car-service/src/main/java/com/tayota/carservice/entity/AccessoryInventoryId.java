package com.tayota.carservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccessoryInventoryId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "dealership_id")
    UUID dealershipId;

    @Column(name = "accessory_id")
    UUID accessoryId;
}
