package com.nguyendin.carservice.repository;

import com.nguyendin.carservice.entity.CarAccessory;
import com.nguyendin.carservice.entity.CarAccessoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarAccessoryRepository extends JpaRepository<CarAccessory, CarAccessoryId> {
    List<CarAccessory> findByIdCarVersionId(UUID carVersionId);
    List<CarAccessory> findByIdAccessoryId(UUID accessoryId);
    boolean existsByIdCarVersionIdAndIdAccessoryId(UUID carVersionId, UUID accessoryId);
}

