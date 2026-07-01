package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.CarSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CarSpecificationRepository extends JpaRepository<CarSpecification, UUID> {
}
