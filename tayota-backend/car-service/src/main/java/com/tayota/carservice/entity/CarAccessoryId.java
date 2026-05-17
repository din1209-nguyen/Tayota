package com.tayota.carservice.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarAccessoryId implements Serializable {
    UUID carVersionId;
    UUID accessoryId;
}
