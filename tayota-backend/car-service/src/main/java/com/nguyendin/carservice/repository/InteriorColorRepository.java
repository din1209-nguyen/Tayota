package com.nguyendin.carservice.repository;

import com.nguyendin.carservice.entity.InteriorColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InteriorColorRepository extends JpaRepository<InteriorColor, UUID> {
    Optional<InteriorColor> findByColorName(String colorName);
    boolean existsByColorName(String colorName);
}

