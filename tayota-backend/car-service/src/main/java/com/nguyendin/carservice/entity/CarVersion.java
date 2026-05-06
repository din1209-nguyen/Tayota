package com.nguyendin.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
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
public class CarVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_series_id", nullable = false)
    private CarSeries carSeries;

    @NotNull
    @Size(max = 50)
    private String version;

    @Column(name = "sale_percent", precision = 5, scale = 2)
    private BigDecimal salePercent = BigDecimal.ZERO;

    @Size(max = 255)
    private String imageUrl;

    @Size(max = 255)
    private String videoUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "carVersion")
    private List<Car> carList;
}
