package com.nguyendin.carservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "\"CAR_GALLERY\"")
@EqualsAndHashCode(of = "id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CarGallery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_version_id", nullable = false)
    private CarVersion carVersion;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;
}
