package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.CarPrice;
import com.tayota.operationservice.entity.car.CarPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarPriceRepository extends JpaRepository<CarPrice, CarPriceId> {
    // Tìm giá xe theo phiên bản
    List<CarPrice> findByCarVersionId(UUID carVersionId);
}
