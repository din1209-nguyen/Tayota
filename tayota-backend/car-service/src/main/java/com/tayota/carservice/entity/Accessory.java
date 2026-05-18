package com.tayota.carservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "\"ACCESSORY\"")
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Accessory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    UUID id;

    @Column(nullable = false, length = 100)
    String model;

    @Column(nullable = false, length = 100)
    String brand;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal price;

    @Column(nullable = false, length = 500)
    String description;

    @Column(nullable = false, length = 500)
    String useContent;

    @Column(nullable = false, length = 500)
    String reminderContent;

    @Column(nullable = false, length = 100)
    String type;
}
