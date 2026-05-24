package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.AccessoryInventory;
import com.tayota.operationservice.entity.car.AccessoryInventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccessoryInventoryRepository extends JpaRepository<AccessoryInventory, AccessoryInventoryId> {
    // Tìm tồn kho phụ kiện theo đại lý
    List<AccessoryInventory> findByDealershipId(UUID dealershipId);
}
