package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.CarGallery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarGalleryRepository extends JpaRepository<CarGallery, UUID> {
    // Tìm hình ảnh theo phiên bản xe
    List<CarGallery> findByCarVersionId(UUID carVersionId);
}
