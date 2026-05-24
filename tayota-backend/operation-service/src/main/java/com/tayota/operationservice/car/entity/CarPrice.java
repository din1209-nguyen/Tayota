package com.tayota.operationservice.car.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "\"CAR_PRICE\"")
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarPrice {

    @EmbeddedId
    CarPriceId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("carVersionId")
    @JoinColumn(name = "car_version_id", nullable = false)
    CarVersion carVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("exteriorColorId")
    @JoinColumn(name = "exterior_color_id", nullable = false)
    ExteriorColor exteriorColor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("interiorColorId")
    @JoinColumn(name = "interior_color_id", nullable = false)
    InteriorColor interiorColor;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal price;

    @Column(name = "ex_image_url", length = 255)
    String exImageUrl;

    @Column(name = "in_image_url", length = 255)
    String inImageUrl;
}
