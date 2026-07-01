package com.tayota.operationservice.service.car;

import com.tayota.operationservice.dto.request.car.CarSeriesRequestDTO;
import com.tayota.operationservice.dto.response.car.CarSeriesResponseDTO;
import com.tayota.operationservice.entity.car.CarSeries;
import com.tayota.operationservice.entity.car.CarStyle;
import com.tayota.operationservice.mapper.car.CarSeriesMapper;
import com.tayota.operationservice.repository.car.CarSeriesRepository;
import com.tayota.operationservice.repository.car.CarStyleRepository;
import com.tayota.operationservice.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarSeriesService {
    private final CarSeriesRepository carSeriesRepository;
    private final CarStyleRepository carStyleRepository;
    private final CarSeriesMapper carSeriesMapper;

    // Lấy danh sách dòng xe
    @Cacheable(value = "carSeriesList", key = "#carStyleId == null ? 'all' : #carStyleId")
    public List<CarSeriesResponseDTO> getCarSeries(String carStyleId) {
        // Chuyển id kiểu dáng sang UUID nếu được truyền vào
        UUID styleId = parseNullableUuid(carStyleId, "Id kiểu dáng không hợp lệ.");

        // Lấy dòng xe theo kiểu dáng hoặc lấy toàn bộ nếu không lọc
        List<CarSeries> series = styleId == null
                ? carSeriesRepository.findAll(Sort.by("name"))
                : carSeriesRepository.findByCarStyleId(styleId);

        // Chuyển danh sách dòng xe sang response
        return series.stream().map(carSeriesMapper::toResponse).toList();
    }

    // Lấy dòng xe theo id
    @Cacheable(value = "carSeriesDetail", key = "#carSeriesId")
    public CarSeriesResponseDTO getCarSeriesById(String carSeriesId) {
        // Tìm dòng xe và chuyển sang response
        return carSeriesMapper.toResponse(findCarSeries(carSeriesId));
    }

    // Thêm dòng xe
    @CacheEvict(
            value = {"carSeriesList", "carSeriesDetail", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail"},
            allEntries = true
    )
    @Transactional
    public CarSeriesResponseDTO createCarSeries(CarSeriesRequestDTO requestDTO) {
        // Tìm kiểu dáng cha của dòng xe
        CarStyle carStyle = findCarStyle(requestDTO.getCarStyleId());

        // Kiểm tra tên dòng xe chưa tồn tại trong cùng kiểu dáng
        if (carSeriesRepository.existsByNameAndCarStyleId(requestDTO.getName(), carStyle.getId())) {
            throw new CustomException(409, "Tên dòng xe đã tồn tại trong kiểu dáng này.");
        }

        // Tạo dòng xe mới
        CarSeries carSeries = CarSeries.builder()
                .carStyle(carStyle)
                .name(requestDTO.getName())
                .description(requestDTO.getDescription())
                .build();

        // Lưu dòng xe vào csdl
        return carSeriesMapper.toResponse(carSeriesRepository.save(carSeries));
    }

    // Cập nhật dòng xe
    @CacheEvict(
            value = {"carSeriesList", "carSeriesDetail", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail"},
            allEntries = true
    )
    @Transactional
    public CarSeriesResponseDTO updateCarSeries(String carSeriesId, CarSeriesRequestDTO requestDTO) {
        // Tìm dòng xe cần cập nhật
        CarSeries carSeries = findCarSeries(carSeriesId);

        // Tìm kiểu dáng cha mới của dòng xe
        CarStyle carStyle = findCarStyle(requestDTO.getCarStyleId());

        // Kiểm tra tên dòng xe chưa bị bản ghi khác sử dụng trong cùng kiểu dáng
        if (carSeriesRepository.existsByNameAndCarStyleIdAndIdNot(requestDTO.getName(), carStyle.getId(), carSeries.getId())) {
            throw new CustomException(409, "Tên dòng xe đã tồn tại trong kiểu dáng này.");
        }

        // Cập nhật thông tin dòng xe
        carSeries.setCarStyle(carStyle);
        carSeries.setName(requestDTO.getName());
        carSeries.setDescription(requestDTO.getDescription());

        // Lưu dòng xe sau khi cập nhật
        return carSeriesMapper.toResponse(carSeriesRepository.save(carSeries));
    }

    // Xóa dòng xe
    @CacheEvict(
            value = {"carSeriesList", "carSeriesDetail", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail"},
            allEntries = true
    )
    @Transactional
    public void deleteCarSeries(String carSeriesId) {
        // Xóa dòng xe theo id
        carSeriesRepository.delete(findCarSeries(carSeriesId));
    }

    // Tìm dòng xe theo id
    private CarSeries findCarSeries(String carSeriesId) {
        // Chuyển id dòng xe sang UUID
        UUID id = parseUuid(carSeriesId, "Id dòng xe không hợp lệ.");

        // Lấy dòng xe từ csdl
        return carSeriesRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy dòng xe."));
    }

    // Tìm kiểu dáng theo id
    private CarStyle findCarStyle(String carStyleId) {
        // Chuyển id kiểu dáng sang UUID
        UUID id = parseUuid(carStyleId, "Id kiểu dáng không hợp lệ.");

        // Lấy kiểu dáng từ csdl
        return carStyleRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy kiểu dáng."));
    }

    // Chuyển chuỗi id sang UUID
    private UUID parseUuid(String value, String message) {
        try {
            // Chuyển chuỗi hợp lệ sang UUID
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            // Trả lỗi nếu chuỗi không đúng định dạng UUID
            throw new CustomException(400, message);
        }
    }

    // Chuyển chuỗi id có thể null sang UUID
    private UUID parseNullableUuid(String value, String message) {
        // Bỏ qua điều kiện lọc nếu id không được truyền vào
        return value == null || value.isBlank() ? null : parseUuid(value, message);
    }
}
