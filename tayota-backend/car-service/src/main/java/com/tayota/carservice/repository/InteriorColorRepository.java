package com.tayota.carservice.repository;

import com.tayota.carservice.entity.InteriorColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InteriorColorRepository extends JpaRepository<InteriorColor, UUID> {
}
