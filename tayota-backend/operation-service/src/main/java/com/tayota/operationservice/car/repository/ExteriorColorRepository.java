package com.tayota.operationservice.car.repository;

import com.tayota.operationservice.car.entity.ExteriorColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExteriorColorRepository extends JpaRepository<ExteriorColor, UUID> {
}
