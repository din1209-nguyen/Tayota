package com.nguyendin.carservice.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarPriceId implements Serializable {

    UUID carVersionId;
    UUID exteriorColorId;
    UUID interiorColorId;
}
