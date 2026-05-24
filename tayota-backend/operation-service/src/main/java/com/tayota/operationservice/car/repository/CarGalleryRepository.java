package com.tayota.operationservice.car.repository;

import com.tayota.operationservice.car.entity.CarGallery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarGalleryRepository extends JpaRepository<CarGallery, UUID> {
    // Tìm hình ảnh theo phiên bản xe
    List<CarGallery> findByCarVersionId(UUID carVersionId);
}
