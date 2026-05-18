package com.tayota.carservice.service;

import com.tayota.carservice.dto.Request.CarSpecificationRequestDTO;
import com.tayota.carservice.dto.Request.CarVersionRequestDTO;
import com.tayota.carservice.dto.Response.CarSpecificationResponseDTO;
import com.tayota.carservice.dto.Response.CarVersionItemResponseDTO;
import com.tayota.carservice.entity.CarSeries;
import com.tayota.carservice.entity.CarSpecification;
import com.tayota.carservice.entity.CarVersion;
import com.tayota.carservice.mapper.CarSpecificationMapper;
import com.tayota.carservice.mapper.CarVersionMapper;
import com.tayota.carservice.repository.CarPriceRepository;
import com.tayota.carservice.repository.CarSeriesRepository;
import com.tayota.carservice.repository.CarSpecificationRepository;
import com.tayota.carservice.repository.CarVersionRepository;
import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarVersionService {
    private final CarVersionRepository carVersionRepository;
    private final CarSeriesRepository carSeriesRepository;
    private final CarSpecificationRepository carSpecificationRepository;
    private final CarPriceRepository carPriceRepository;
    private final CarVersionMapper carVersionMapper;
    private final CarSpecificationMapper carSpecificationMapper;

    // Lấy danh sách phiên bản xe
    @Cacheable(value = "carVersionList", key = "#carSeriesId == null ? 'all' : #carSeriesId")
    public List<CarVersionItemResponseDTO> getCarVersions(String carSeriesId) {
        // Chuyển id dòng xe sang UUID nếu được truyền vào
        UUID seriesId = parseNullableUuid(carSeriesId, "Id dòng xe không hợp lệ.");

        // Lấy phiên bản xe theo dòng xe hoặc lấy toàn bộ nếu không lọc
        List<CarVersion> versions = seriesId == null
                ? carVersionRepository.findAll(Sort.by("modelYear").descending().and(Sort.by("name")))
                : carVersionRepository.findByCarSeriesId(seriesId);

        // Chuyển danh sách phiên bản xe sang response
        return versions.stream().map(this::mapToItem).toList();
    }

    // Thêm phiên bản xe
    @CacheEvict(
            value = {"carVersionList", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail", "catalogSpecification"},
            allEntries = true
    )
    @Transactional
    public CarVersionItemResponseDTO createCarVersion(CarVersionRequestDTO requestDTO) {
        // Tìm dòng xe cha của phiên bản xe
        CarSeries carSeries = findCarSeries(requestDTO.getCarSeriesId());

        // Kiểm tra tên phiên bản chưa tồn tại trong cùng dòng xe
        if (carVersionRepository.existsByNameAndCarSeriesId(requestDTO.getName(), carSeries.getId())) {
            throw new CustomException(409, "Tên phiên bản đã tồn tại trong dòng xe này.");
        }

        // Tạo phiên bản xe mới
        CarVersion carVersion = CarVersion.builder()
                .carSeries(carSeries)
                .name(requestDTO.getName())
                .salePercent(requestDTO.getSalePercent())
                .modelYear(requestDTO.getModelYear())
                .videoUrl(requestDTO.getVideoUrl())
                .build();

        // Lưu phiên bản xe vào csdl
        return mapToItem(carVersionRepository.save(carVersion));
    }

    // Cập nhật phiên bản xe
    @CacheEvict(
            value = {"carVersionList", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail", "catalogSpecification"},
            allEntries = true
    )
    @Transactional
    public CarVersionItemResponseDTO updateCarVersion(String carVersionId, CarVersionRequestDTO requestDTO) {
        // Tìm phiên bản xe cần cập nhật
        CarVersion carVersion = findCarVersion(carVersionId);

        // Tìm dòng xe cha mới của phiên bản xe
        CarSeries carSeries = findCarSeries(requestDTO.getCarSeriesId());

        // Kiểm tra tên phiên bản chưa bị bản ghi khác sử dụng trong cùng dòng xe
        if (carVersionRepository.existsByNameAndCarSeriesIdAndIdNot(requestDTO.getName(), carSeries.getId(), carVersion.getId())) {
            throw new CustomException(409, "Tên phiên bản đã tồn tại trong dòng xe này.");
        }

        // Cập nhật thông tin phiên bản xe
        carVersion.setCarSeries(carSeries);
        carVersion.setName(requestDTO.getName());
        carVersion.setSalePercent(requestDTO.getSalePercent());
        carVersion.setModelYear(requestDTO.getModelYear());
        carVersion.setVideoUrl(requestDTO.getVideoUrl());

        // Lưu phiên bản xe sau khi cập nhật
        return mapToItem(carVersionRepository.save(carVersion));
    }

    // Xóa phiên bản xe
    @CacheEvict(
            value = {"carVersionList", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail", "catalogSpecification"},
            allEntries = true
    )
    @Transactional
    public void deleteCarVersion(String carVersionId) {
        // Xóa phiên bản xe theo id
        carVersionRepository.delete(findCarVersion(carVersionId));
    }

    // Lưu thông số kỹ thuật của xe
    @CacheEvict(value = {"catalogVersionDetail", "catalogSpecification"}, allEntries = true)
    @Transactional
    public CarSpecificationResponseDTO saveSpecification(String carVersionId, CarSpecificationRequestDTO requestDTO) {
        // Tìm phiên bản xe cần lưu thông số
        CarVersion carVersion = findCarVersion(carVersionId);

        // Lấy thông số hiện có hoặc tạo mới nếu chưa tồn tại
        CarSpecification specification = carSpecificationRepository.findById(carVersion.getId())
                .orElseGet(() -> CarSpecification.builder().carVersion(carVersion).build());

        // Cập nhật thông số kỹ thuật
        specification.setOrigin(requestDTO.getOrigin());
        specification.setFuel(requestDTO.getFuel());
        specification.setNumberOfSeats(requestDTO.getNumberOfSeats());
        specification.setLength(requestDTO.getLength());
        specification.setWidth(requestDTO.getWidth());
        specification.setHeight(requestDTO.getHeight());
        specification.setCapacity(requestDTO.getCapacity());
        specification.setCylinderCapacity(requestDTO.getCylinderCapacity());
        specification.setCylinder(requestDTO.getCylinder());
        specification.setGearbox(requestDTO.getGearbox());
        specification.setMaximumSpeed(requestDTO.getMaximumSpeed());
        specification.setAcceleration(requestDTO.getAcceleration());
        specification.setTorque(requestDTO.getTorque());
        specification.setGrossWeightAllowance(requestDTO.getGrossWeightAllowance());
        specification.setTrademarks(requestDTO.getTrademarks());

        // Lưu thông số kỹ thuật vào csdl
        return carSpecificationMapper.toResponse(carSpecificationRepository.save(specification));
    }

    // Tìm phiên bản xe theo id
    private CarVersion findCarVersion(String carVersionId) {
        // Chuyển id phiên bản xe sang UUID
        UUID id = parseUuid(carVersionId, "Id phiên bản xe không hợp lệ.");

        // Lấy phiên bản xe từ csdl
        return carVersionRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiên bản xe."));
    }

    // Tìm dòng xe theo id
    private CarSeries findCarSeries(String carSeriesId) {
        // Chuyển id dòng xe sang UUID
        UUID id = parseUuid(carSeriesId, "Id dòng xe không hợp lệ.");

        // Lấy dòng xe từ csdl
        return carSeriesRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy dòng xe."));
    }

    // Chuyển phiên bản xe sang response danh sách
    private CarVersionItemResponseDTO mapToItem(CarVersion carVersion) {
        // Lấy giá thấp nhất của phiên bản xe
        BigDecimal minPrice = carPriceRepository.findByCarVersionId(carVersion.getId())
                .stream()
                .map(price -> price.getPrice())
                .min(Comparator.naturalOrder())
                .orElse(null);

        // Trả về response rút gọn cho phiên bản xe
        return carVersionMapper.toItem(carVersion, minPrice, null);
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
