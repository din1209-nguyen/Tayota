package com.nguyendin.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull; //nếu dùng ANTLR thì validation sẽ không hoạt động
import lombok.*;

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
public class CarStyle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Size(max = 100)
    private String name;

    @NotNull
    @Size(max = 250)
    private String description;

    @OneToMany(mappedBy = "carStyle")
    private List<CarSeries> carSeriesList;
}
