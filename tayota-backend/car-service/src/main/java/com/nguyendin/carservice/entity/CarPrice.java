package com.nguyendin.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "\"CAR_PRICE\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CarPrice {

    @EmbeddedId
    private CarPriceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("carVersionId")
    @JoinColumn(name = "car_version_id")
    private CarVersion carVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("exteriorColorId")
    @JoinColumn(name = "exterior_color_id")
    private ExteriorColor exteriorColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("interiorColorId")
    @JoinColumn(name = "interior_color_id")
    private InteriorColor interiorColor;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Size(max = 255)
    private String exImageUrl;

    @Size(max = 255)
    private String inImageUrl;
}
