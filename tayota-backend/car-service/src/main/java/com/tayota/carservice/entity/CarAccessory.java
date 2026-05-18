package com.tayota.carservice.entity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "\"CAR_ACCESSORY\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarAccessory {

    @EmbeddedId
    CarAccessoryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("carVersionId")
    @JoinColumn(name = "car_version_id")
    CarVersion carVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("accessoryId")
    @JoinColumn(name = "accessory_id")
    Accessory accessory;
}
