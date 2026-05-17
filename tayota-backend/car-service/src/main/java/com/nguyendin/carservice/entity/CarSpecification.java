package com.nguyendin.carservice.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "\"CAR_SPECIFICATION\"")
@EqualsAndHashCode(of = "carVersionId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarSpecification {

    @Id
    @Column(name = "car_version_id")
    UUID carVersionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "car_version_id")
    CarVersion carVersion;

    @NotNull
    @Size(max = 100)
    String origin;

    @NotNull
    @Size(max = 50)
    String fuel;

    @NotNull
    Integer numberOfSeats;

    @NotNull
    Integer length;

    @NotNull
    Integer width;

    @NotNull
    Integer height;

    Integer capacity;

    @Size(max = 50)
    String cylinderCapacity;

    Integer cylinder;

    @Size(max = 50)
    String gearbox;

    Integer maximumSpeed;

    @Size(max = 50)
    String acceleration;

    @Size(max = 100)
    String torque;

    Integer grossWeightAllowance;

    @Size(max = 100)
    String trademarks;
}
