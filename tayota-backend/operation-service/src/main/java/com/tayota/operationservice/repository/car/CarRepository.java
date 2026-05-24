package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarRepository extends JpaRepository<Car, String>, JpaSpecificationExecutor<Car> {
    // Tìm xe vật lý theo chủ sở hữu
    List<Car> findByOwnerUserId(UUID ownerUserId);

    // Kiểm tra số máy đã tồn tại
    boolean existsByEngineNumber(String engineNumber);

    // Kiểm tra số máy đã tồn tại ngoài xe hiện tại
    boolean existsByEngineNumberAndVinIdNot(String engineNumber, String vinId);
}
