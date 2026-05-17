package com.tayota.carservice.repository;

import com.tayota.carservice.entity.CarStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarStyleRepository extends JpaRepository<CarStyle, UUID> {
    Optional<CarStyle> findByName(String name);
    List<CarStyle> findByNameContainingIgnoreCase(String name);
    boolean existsByName(String name);
}

