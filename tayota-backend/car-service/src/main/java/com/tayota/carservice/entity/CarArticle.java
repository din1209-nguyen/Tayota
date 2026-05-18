package com.tayota.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "\"CAR_ARTICLE\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_version_id", nullable = false)
    CarVersion carVersion;

    @NotNull
    @Size(max = 50)
    String type;

    @NotNull
    @Size(max = 255)
    String title;

    @NotNull
    @Column(columnDefinition = "TEXT")
    String content;

    @Size(max = 255)
    String imageUrl;
}
