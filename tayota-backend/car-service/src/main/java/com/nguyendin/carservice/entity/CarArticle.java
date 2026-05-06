package com.nguyendin.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "\"CAR_ARTICLE\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CarArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_version_id", nullable = false)
    private CarVersion carVersion;

    @NotNull
    @Size(max = 50)
    private String type;

    @NotNull
    @Size(max = 255)
    private String title;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String content;

    @Size(max = 255)
    private String imageUrl;
}
