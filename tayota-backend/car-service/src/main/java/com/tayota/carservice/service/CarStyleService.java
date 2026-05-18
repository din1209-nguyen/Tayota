package com.tayota.carservice.service;

import com.tayota.carservice.dto.Request.CarStyleRequestDTO;
import com.tayota.carservice.dto.Response.CarStyleResponseDTO;
import com.tayota.carservice.entity.CarStyle;
import com.tayota.carservice.mapper.CarStyleMapper;
import com.tayota.carservice.repository.CarStyleRepository;
import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarStyleService {
    private final CarStyleRepository carStyleRepository;
    private final CarStyleMapper carStyleMapper;

    // Lấy danh sách kiểu xe
    @Cacheable(value = "carStyleList", key = "'all'")
    public List<CarStyleResponseDTO> getCarStyles() {
        // Lấy toàn bộ kiểu xe từ csdl
        return carStyleRepository.findAll()
                .stream()
                .map(carStyleMapper::toResponse)
                .toList();
    }

    // Lấy kiểu xe theo id
    @Cacheable(value = "carStyleDetail", key = "#carStyleId")
    public CarStyleResponseDTO getCarStyle(String carStyleId) {
        // Tìm kiểu xe theo id
        CarStyle carStyle = findCarStyleById(carStyleId);

        // Chuyển kiểu xe sang response
        return carStyleMapper.toResponse(carStyle);
    }

    // Thêm kiểu xe
    @CacheEvict(
            value = {"carStyleList", "carStyleDetail", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail"},
            allEntries = true
    )
    @Transactional
    public CarStyleResponseDTO createCarStyle(CarStyleRequestDTO requestDTO) {
        // Kiểm tra tên kiểu xe chưa tồn tại
        if (carStyleRepository.existsByName(requestDTO.getName())) {
            throw new CustomException(409, "Tên kiểu xe đã tồn tại.");
        }

        // Tạo kiểu xe mới từ request
        CarStyle carStyle = CarStyle.builder()
                .name(requestDTO.getName())
                .description(requestDTO.getDescription())
                .build();

        // Lưu kiểu xe vào csdl
        return carStyleMapper.toResponse(carStyleRepository.save(carStyle));
    }

    // Cập nhật kiểu xe
    @CacheEvict(
            value = {"carStyleList", "carStyleDetail", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail"},
            allEntries = true
    )
    @Transactional
    public CarStyleResponseDTO updateCarStyle(String carStyleId, CarStyleRequestDTO requestDTO) {
        CarStyle carStyle = findCarStyleById(carStyleId);
        UUID id = carStyle.getId();

        // Kiểm tra tên kiểu xe chưa bị bản ghi khác sử dụng
        if (carStyleRepository.existsByNameAndIdNot(requestDTO.getName(), id)) {
            throw new CustomException(409, "Tên kiểu xe đã tồn tại.");
        }

        // Cập nhật thông tin kiểu xe
        carStyle.setName(requestDTO.getName());
        carStyle.setDescription(requestDTO.getDescription());

        // Lưu kiểu xe sau khi cập nhật
        return carStyleMapper.toResponse(carStyleRepository.save(carStyle));
    }

    // Xóa kiểu xe
    @CacheEvict(
            value = {"carStyleList", "carStyleDetail", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail"},
            allEntries = true
    )
    @Transactional
    public void deleteCarStyle(String carStyleId) {
        // Tìm kiểu xe cần xóa
        CarStyle carStyle = findCarStyleById(carStyleId);

        // Xóa kiểu xe khỏi csdl
        carStyleRepository.delete(carStyle);
    }

    // Tìm kiểu xe theo id
    private CarStyle findCarStyleById(String carStyleId) {
        // Chuyển id kiểu xe sang UUID
        UUID id = parseUuid(carStyleId);

        // Lấy kiểu xe từ csdl
        return carStyleRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy kiểu xe."));
    }

    // Chuyển chuỗi id sang UUID
    private UUID parseUuid(String carStyleId) {
        try {
            // Chuyển chuỗi hợp lệ sang UUID
            return UUID.fromString(carStyleId);
        } catch (IllegalArgumentException exception) {
            // Trả lỗi nếu chuỗi không đúng định dạng UUID
            throw new CustomException(400, "Id kiểu xe không hợp lệ.");
        }
    }

}
