package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.CarAccessory;
import com.tayota.operationservice.entity.car.CarAccessoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarAccessoryRepository extends JpaRepository<CarAccessory, CarAccessoryId> {
    // Tìm phụ kiện theo phiên bản xe
    List<CarAccessory> findByCarVersionId(UUID carVersionId);
}