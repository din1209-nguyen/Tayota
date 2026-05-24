package com.tayota.operationservice.car.repository;

import com.tayota.operationservice.car.entity.AccessoryInventory;
import com.tayota.operationservice.car.entity.AccessoryInventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccessoryInventoryRepository extends JpaRepository<AccessoryInventory, AccessoryInventoryId> {
    // Tìm tồn kho phụ kiện theo đại lý
    List<AccessoryInventory> findByDealershipId(UUID dealershipId);
}
