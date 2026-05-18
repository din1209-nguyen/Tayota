package com.tayota.carservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarPriceId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "car_version_id")
    UUID carVersionId;

    @Column(name = "exterior_color_id")
    UUID exteriorColorId;

    @Column(name = "interior_color_id")
    UUID interiorColorId;
}
