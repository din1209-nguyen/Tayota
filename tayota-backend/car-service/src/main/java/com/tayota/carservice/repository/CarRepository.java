package com.tayota.carservice.repository;

import com.tayota.carservice.entity.Car;
import com.tayota.carservice.enums.CarStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarRepository extends JpaRepository<Car, UUID> {
    List<Car> findByStatus(CarStatusType status);
    List<Car> findByCarVersionId(UUID carVersionId);
    List<Car> findByProductionYear(int productionYear);
    boolean existsByEngineNumber(String engineNumber);
}

