package com.nguyendin.carservice.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CarPriceId implements Serializable {

    private UUID carVersionId;
    private UUID exteriorColorId;
    private UUID interiorColorId;
}
