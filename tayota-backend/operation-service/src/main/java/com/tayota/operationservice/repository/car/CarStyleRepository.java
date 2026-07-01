package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.CarStyle;
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
