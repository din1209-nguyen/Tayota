package com.nguyendin.carservice.repository;

import com.nguyendin.carservice.entity.ExteriorColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExteriorColorRepository extends JpaRepository<ExteriorColor, UUID> {
    Optional<ExteriorColor> findByColorName(String colorName);
    boolean existsByColorName(String colorName);
}

