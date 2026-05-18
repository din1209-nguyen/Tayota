package com.tayota.carservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "\"CAR_SPECIFICATION\"")
@EqualsAndHashCode(of = "carVersionId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarSpecification {

    @Id
    @Column(name = "car_version_id")
    UUID carVersionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "car_version_id")
    CarVersion carVersion;

    @Column(nullable = false, length = 100)
    String origin;

    @Column(nullable = false, length = 50)
    String fuel;

    @Column(nullable = false)
    Integer numberOfSeats;

    @Column(nullable = false)
    Integer length;

    @Column(nullable = false)
    Integer width;

    @Column(nullable = false)
    Integer height;

    Integer capacity;

    @Column(length = 50)
    String cylinderCapacity;

    Integer cylinder;

    @Column(length = 50)
    String gearbox;

    Integer maximumSpeed;

    @Column(length = 50)
    String acceleration;

    @Column(length = 100)
    String torque;

    Integer grossWeightAllowance;

    @Column(length = 100)
    String trademarks;
}
