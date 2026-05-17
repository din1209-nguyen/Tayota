package com.nguyendin.carservice.entity;

import com.nguyendin.carservice.enums.CarStatusType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_version_id", nullable = false)
    CarVersion carVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealership_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    Dealership dealership;

    @NotNull
    @Size(max = 50)
    String engineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CarStatusType status = CarStatusType.IN_STOCK;

    int mileage;

    @NotNull
    int productionYear;

    LocalDate entryDate;

    @CreationTimestamp
    LocalDateTime createdAt;


}
