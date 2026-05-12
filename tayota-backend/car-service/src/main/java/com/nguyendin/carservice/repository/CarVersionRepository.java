package com.nguyendin.carservice.repository;

import com.nguyendin.carservice.entity.CarVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarVersionRepository extends JpaRepository<CarVersion, UUID> {
    List<CarVersion> findByCarSeriesId(UUID carSeriesId);
    List<CarVersion> findByVersionContainingIgnoreCase(String version);
    boolean existsByCarSeriesIdAndVersion(UUID carSeriesId, String version);
}

