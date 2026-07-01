package com.tayota.operationservice.entity.car;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
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
    @Column(updatable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_series_id", nullable = false)
    CarSeries carSeries;

    @Column(nullable = false, length = 50)
    String name;

    @Column(name = "sale_percent", precision = 5, scale = 2)
    @Builder.Default
    BigDecimal salePercent = BigDecimal.ZERO;

    @Column(name = "model_year", nullable = false)
    Integer modelYear;

    @Column(name = "image_url", length = 1024)
    String imageUrl;

    @Column(length = 1024)
    String videoUrl;

    @Builder.Default
    @ColumnDefault("true")
    @Column(name = "is_visible", nullable = false)
    boolean visible = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    Instant createdAt;
}
