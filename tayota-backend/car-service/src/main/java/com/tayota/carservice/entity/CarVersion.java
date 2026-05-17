package com.tayota.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(name = "\"CAR_VERSION\"")
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_series_id", nullable = false)
    CarSeries carSeries;

    @NotNull
    @Size(max = 50)
    String version;

    @Column(name = "sale_percent", precision = 5, scale = 2)
    BigDecimal salePercent = BigDecimal.ZERO;

    @Size(max = 255)
    String imageUrl;

    @Size(max = 255)
    String videoUrl;

    @CreationTimestamp
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "carVersion")
    List<Car> carList;
}
