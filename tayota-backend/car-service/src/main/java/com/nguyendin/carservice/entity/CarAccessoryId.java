package com.nguyendin.carservice.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CarAccessoryId implements Serializable {
    private UUID carVersionId;
    private UUID accessoryId;
}
