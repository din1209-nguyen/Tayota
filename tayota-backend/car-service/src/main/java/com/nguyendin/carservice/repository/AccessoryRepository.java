package com.nguyendin.carservice.repository;

import com.nguyendin.carservice.entity.Accessory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccessoryRepository extends JpaRepository<Accessory, UUID> {
    List<Accessory> findByType(String type);
    List<Accessory> findByBrand(String brand);
    List<Accessory> findByBrandAndType(String brand, String type);
    boolean existsByModelAndBrand(String model, String brand);
}

