package com.nguyendin.carservice.entity;

import com.nguyendin.carservice.enums.CarStatusType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(name = "\"CAR\"")
@Builder
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_version_id", nullable = false)
    private CarVersion carVersion;

    @org.jetbrains.annotations.NotNull
    @Size(max = 50)
    private String engineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarStatusType status = CarStatusType.IN_STOCK;

    private int mileage;

    @NotNull
    private int productionYear;

    private LocalDate entryDate;

    @CreationTimestamp
    private LocalDateTime createdAt;


}
