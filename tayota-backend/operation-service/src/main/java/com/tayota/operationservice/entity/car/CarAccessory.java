package com.tayota.operationservice.entity.car;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "\"CAR_ACCESSORY\"")
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarAccessory {

    @EmbeddedId
    CarAccessoryId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("carVersionId")
    @JoinColumn(name = "car_version_id", nullable = false)
    CarVersion carVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("accessoryId")
    @JoinColumn(name = "accessory_id", nullable = false)
    Accessory accessory;
}
