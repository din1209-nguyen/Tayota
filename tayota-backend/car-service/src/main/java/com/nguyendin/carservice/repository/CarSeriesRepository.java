package com.nguyendin.carservice.repository;

import com.nguyendin.carservice.entity.CarSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarSeriesRepository extends JpaRepository<CarSeries, UUID> {
    List<CarSeries> findByCarStyleId(UUID carStyleId);
    List<CarSeries> findByNameContainingIgnoreCase(String name);
}

