package com.nguyendin.carservice.repository;

import com.nguyendin.carservice.entity.CarPrice;
import com.nguyendin.carservice.entity.CarPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarPriceRepository extends JpaRepository<CarPrice, CarPriceId> {
    List<CarPrice> findByIdCarVersionId(UUID carVersionId);
    List<CarPrice> findByIdExteriorColorId(UUID exteriorColorId);
    List<CarPrice> findByIdInteriorColorId(UUID interiorColorId);
    List<CarPrice> findByIdCarVersionIdAndIdExteriorColorIdAndIdInteriorColorId(
            UUID carVersionId, UUID exteriorColorId, UUID interiorColorId);
}

