package com.nguyendin.carservice.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"CAR_ACCESSORY\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CarAccessory {

    @EmbeddedId
    private CarAccessoryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("carVersionId")
    @JoinColumn(name = "car_version_id")
    private CarVersion carVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("accessoryId")
    @JoinColumn(name = "accessory_id")
    private Accessory accessory;
}
