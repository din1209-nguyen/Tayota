package com.nguyendin.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(name = "\"CAR_SERIES\"")
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarSeries {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_style_id", nullable = false)
    CarStyle carStyle;

    @NotNull
    @Size(max = 100)
    String name;

    @NotNull
    @Size(max = 250)
    String description;

    @CreationTimestamp
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "carSeries")
    List<CarVersion> carVersionList;
}
