package com.nguyendin.carservice.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "\"CAR_SPECIFICATION\"")
@EqualsAndHashCode(of = "carVersionId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CarSpecification {

    @Id
    @Column(name = "car_version_id")
    private UUID carVersionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "car_version_id")
    private CarVersion carVersion;

    @NotNull
    @Size(max = 100)
    private String origin;

    @NotNull
    @Size(max = 50)
    private String fuel;

    @NotNull
    private Integer numberOfSeats;

    @NotNull
    private Integer length;

    @NotNull
    private Integer width;

    @NotNull
    private Integer height;

    private Integer capacity;

    @Size(max = 50)
    private String cylinderCapacity;

    private Integer cylinder;

    @Size(max = 50)
    private String gearbox;

    private Integer maximumSpeed;

    @Size(max = 50)
    private String acceleration;

    @Size(max = 100)
    private String torque;

    private Integer grossWeightAllowance;

    @Size(max = 100)
    private String trademarks;
}
