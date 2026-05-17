package com.tayota.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "\"CAR_PRICE\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarPrice {

    @EmbeddedId
    CarPriceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("carVersionId")
    @JoinColumn(name = "car_version_id")
    CarVersion carVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("exteriorColorId")
    @JoinColumn(name = "exterior_color_id")
    ExteriorColor exteriorColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("interiorColorId")
    @JoinColumn(name = "interior_color_id")
    InteriorColor interiorColor;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal price;

    @Size(max = 255)
    String exImageUrl;

    @Size(max = 255)
    String inImageUrl;
}
