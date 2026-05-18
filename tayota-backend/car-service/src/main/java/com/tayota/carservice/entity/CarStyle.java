package com.tayota.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull; //nếu dùng ANTLR thì validation sẽ không hoạt động
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(name = "\"CAR_STYLE\"")
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarStyle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @NotNull
    @Size(max = 100)
    String name;

    @NotNull
    @Size(max = 250)
    String description;

    @OneToMany(mappedBy = "carStyle")
    List<CarSeries> carSeriesList;
}
