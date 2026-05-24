package com.tayota.operationservice.entity.car;

import com.tayota.operationservice.enums.car.CarStatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "vinId")
@Table(name = "\"CAR\"")
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Car {
    @Id
    @Column(name = "vin_id", length = 17)
    String vinId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_version_id", nullable = false)
    CarVersion carVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealership_id", nullable = false)
    Dealership dealership;

    @Column(name = "engine_number", nullable = false, unique = true, length = 50)
    String engineNumber;

    @Column(name = "owner_user_id")
    UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    CarStatusType status = CarStatusType.IN_STOCK;

    @Column(name = "producted_year", nullable = false)
    Instant productedYear;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    Instant createdAt;
}
