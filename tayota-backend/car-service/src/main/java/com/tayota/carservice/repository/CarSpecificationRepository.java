package com.tayota.carservice.repository;

import com.tayota.carservice.entity.CarSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CarSpecificationRepository extends JpaRepository<CarSpecification, UUID> {
    boolean existsByCarVersionId(UUID carVersionId);
}

