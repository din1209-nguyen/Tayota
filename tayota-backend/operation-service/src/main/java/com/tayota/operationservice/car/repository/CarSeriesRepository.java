package com.tayota.operationservice.car.repository;

import com.tayota.operationservice.car.entity.CarSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarSeriesRepository extends JpaRepository<CarSeries, UUID> {
    // Tìm dòng xe theo kiểu dáng
    List<CarSeries> findByCarStyleId(UUID carStyleId);

    // Kiểm tra tên dòng xe đã tồn tại trong cùng kiểu dáng
    boolean existsByNameAndCarStyleId(String name, UUID carStyleId);

    // Kiểm tra tên dòng xe đã tồn tại trong cùng kiểu dáng ngoài bản ghi hiện tại
    boolean existsByNameAndCarStyleIdAndIdNot(String name, UUID carStyleId, UUID id);
}
