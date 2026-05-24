package com.tayota.operationservice.car.repository;

import com.tayota.operationservice.car.entity.CarAccessory;
import com.tayota.operationservice.car.entity.CarAccessoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarAccessoryRepository extends JpaRepository<CarAccessory, CarAccessoryId> {
    // Tìm phụ kiện theo phiên bản xe
    List<CarAccessory> findByCarVersionId(UUID carVersionId);
}
