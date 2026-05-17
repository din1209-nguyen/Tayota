package com.tayota.carservice.repository;

import com.tayota.carservice.entity.CarGallery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarGalleryRepository extends JpaRepository<CarGallery, UUID> {
    List<CarGallery> findByCarVersionId(UUID carVersionId);
    long countByCarVersionId(UUID carVersionId);
}

