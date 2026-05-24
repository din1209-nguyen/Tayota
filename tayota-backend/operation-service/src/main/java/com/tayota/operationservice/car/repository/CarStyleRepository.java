package com.tayota.operationservice.car.repository;

import com.tayota.operationservice.car.entity.CarStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarStyleRepository extends JpaRepository<CarStyle, UUID> {
    // Tìm kiểu xe theo tên
    Optional<CarStyle> findByName(String name);

    // Tìm kiểu xe theo từ khóa trong tên
    List<CarStyle> findByNameContainingIgnoreCase(String name);

    // Kiểm tra tên kiểu xe đã tồn tại
    boolean existsByName(String name);

    // Kiểm tra tên kiểu xe đã tồn tại ngoài bản ghi hiện tại
    boolean existsByNameAndIdNot(String name, UUID id);
}
