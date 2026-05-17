package com.tayota.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "\"ACCESSORY\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Accessory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @NotNull
    @Size(max = 100)
    String model;

    @NotNull
    @Size(max = 100)
    String brand;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal price;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(columnDefinition = "TEXT")
    String useContent;

    @Column(columnDefinition = "TEXT")
    String reminderContent;

    @NotNull
    @Size(max = 100)
    String type;
}
