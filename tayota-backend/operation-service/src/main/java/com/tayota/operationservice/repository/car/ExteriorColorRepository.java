package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.ExteriorColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExteriorColorRepository extends JpaRepository<ExteriorColor, UUID> {
    Optional<ExteriorColor> findByColorNameIgnoreCase(String colorName);
}
