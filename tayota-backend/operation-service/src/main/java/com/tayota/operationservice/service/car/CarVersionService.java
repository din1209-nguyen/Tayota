package com.tayota.operationservice.service.car;

import com.tayota.operationservice.dto.request.car.CarSpecificationRequestDTO;
import com.tayota.operationservice.dto.request.car.CarGalleryRequestDTO;
import com.tayota.operationservice.dto.request.car.CarPriceRequestDTO;
import com.tayota.operationservice.dto.request.car.CarVersionRequestDTO;
import com.tayota.operationservice.dto.response.car.CarSpecificationResponseDTO;
import com.tayota.operationservice.dto.response.car.CarGalleryResponseDTO;
import com.tayota.operationservice.dto.response.car.CarPriceResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionItemResponseDTO;
import com.tayota.operationservice.entity.car.CarSeries;
import com.tayota.operationservice.entity.car.CarGallery;
import com.tayota.operationservice.entity.car.CarPrice;
import com.tayota.operationservice.entity.car.CarPriceId;
import com.tayota.operationservice.entity.car.ExteriorColor;
import com.tayota.operationservice.entity.car.InteriorColor;
import com.tayota.operationservice.entity.car.CarSpecification;
import com.tayota.operationservice.entity.car.CarVersion;
import com.tayota.operationservice.mapper.car.CarSpecificationMapper;
import com.tayota.operationservice.mapper.car.CarGalleryMapper;
import com.tayota.operationservice.mapper.car.CarPriceMapper;
import com.tayota.operationservice.mapper.car.CarVersionMapper;
import com.tayota.operationservice.repository.car.CarPriceRepository;
import com.tayota.operationservice.repository.car.CarGalleryRepository;
import com.tayota.operationservice.repository.car.ExteriorColorRepository;
import com.tayota.operationservice.repository.car.InteriorColorRepository;
import com.tayota.operationservice.repository.car.CarSeriesRepository;
import com.tayota.operationservice.repository.car.CarSpecificationRepository;
import com.tayota.operationservice.repository.car.CarVersionRepository;
import com.tayota.operationservice.exception.CustomException;
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
    private final CarGalleryRepository carGalleryRepository;
    private final ExteriorColorRepository exteriorColorRepository;
    private final InteriorColorRepository interiorColorRepository;
    private final CarVersionMapper carVersionMapper;
    private final CarSpecificationMapper carSpecificationMapper;
    private final CarGalleryMapper carGalleryMapper;
    private final CarPriceMapper carPriceMapper;

    // Lấy danh sách phiên bản xe
    @Cacheable(value = "carVersionList", key = "#carSeriesId == null ? 'all' : #carSeriesId")
    public List<CarVersionItemResponseDTO> getCarVersions(String carSeriesId) {
        // Chuyển id dòng xe sang UUID nếu được truyền vào
        UUID seriesId = parseNullableUuid(carSeriesId, "Id dòng xe không hợp lệ.");

        // Lấy phiên bản xe theo dòng xe hoặc lấy toàn bộ nếu không lọc
        List<CarVersion> versions = seriesId == null
                ? carVersionRepository.findByVisibleTrue(Sort.by("modelYear").descending().and(Sort.by("name")))
                : carVersionRepository.findByCarSeriesId(seriesId).stream().filter(CarVersion::isVisible).toList();

        // Chuyển danh sách phiên bản xe sang response
        return versions.stream().map(this::mapToItem).toList();
    }

    public List<CarVersionItemResponseDTO> getCarVersionsForManagement() {
        return carVersionRepository.findAll(Sort.by("modelYear").descending().and(Sort.by("name")))
                .stream().map(this::mapToItem).toList();
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
                .visible(requestDTO.getVisible() == null || requestDTO.getVisible())
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
        if (requestDTO.getVisible() != null) {
            carVersion.setVisible(requestDTO.getVisible());
        }

        // Lưu phiên bản xe sau khi cập nhật
        return mapToItem(carVersionRepository.save(carVersion));
    }

    // Ẩn phiên bản xe để giữ dữ liệu catalog đã được sử dụng.
    @CacheEvict(
            value = {"carVersionList", "catalogStylesWithVersions", "catalogVersionSearch", "catalogVersionDetail", "catalogSpecification"},
            allEntries = true
    )
    @Transactional
    public void deleteCarVersion(String carVersionId) {
        CarVersion carVersion = findCarVersion(carVersionId);
        carVersion.setVisible(false);
        carVersionRepository.save(carVersion);
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

    @CacheEvict(value = {"catalogStylesWithVersions", "catalogVersionDetail"}, allEntries = true)
    @Transactional
    public CarPriceResponseDTO savePrice(String carVersionId, CarPriceRequestDTO request) {
        CarVersion carVersion = findCarVersion(carVersionId);
        UUID exteriorId = parseUuid(request.getExteriorColorId(), "Id màu ngoại thất không hợp lệ.");
        UUID interiorId = parseUuid(request.getInteriorColorId(), "Id màu nội thất không hợp lệ.");
        ExteriorColor exteriorColor = exteriorColorRepository.findById(exteriorId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy màu ngoại thất."));
        InteriorColor interiorColor = interiorColorRepository.findById(interiorId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy màu nội thất."));
        CarPriceId id = new CarPriceId(carVersion.getId(), exteriorId, interiorId);
        CarPrice price = carPriceRepository.findById(id).orElseGet(() -> CarPrice.builder()
                .id(id).carVersion(carVersion).exteriorColor(exteriorColor).interiorColor(interiorColor).build());
        price.setPrice(request.getPrice());
        price.setExImageUrl(request.getExImageUrl());
        price.setInImageUrl(request.getInImageUrl());
        return carPriceMapper.toResponse(carPriceRepository.save(price));
    }

    @CacheEvict(value = {"catalogStylesWithVersions", "catalogVersionDetail"}, allEntries = true)
    @Transactional
    public void deletePrice(String carVersionId, String exteriorColorId, String interiorColorId) {
        UUID versionId = findCarVersion(carVersionId).getId();
        carPriceRepository.deleteById(new CarPriceId(
                versionId,
                parseUuid(exteriorColorId, "Id màu ngoại thất không hợp lệ."),
                parseUuid(interiorColorId, "Id màu nội thất không hợp lệ.")
        ));
    }

    @CacheEvict(value = "catalogVersionDetail", allEntries = true)
    @Transactional
    public CarGalleryResponseDTO addGallery(String carVersionId, CarGalleryRequestDTO request) {
        return carGalleryMapper.toResponse(carGalleryRepository.save(CarGallery.builder()
                .carVersion(findCarVersion(carVersionId)).imageUrl(request.getImageUrl()).build()));
    }

    @CacheEvict(value = "catalogVersionDetail", allEntries = true)
    @Transactional
    public CarGalleryResponseDTO updateGallery(String carVersionId, String galleryId, CarGalleryRequestDTO request) {
        CarVersion carVersion = findCarVersion(carVersionId);
        CarGallery gallery = carGalleryRepository.findById(parseUuid(galleryId, "Id ảnh không hợp lệ."))
                .filter(item -> item.getCarVersion().getId().equals(carVersion.getId()))
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy ảnh."));
        gallery.setImageUrl(request.getImageUrl());
        return carGalleryMapper.toResponse(carGalleryRepository.save(gallery));
    }

    @CacheEvict(value = "catalogVersionDetail", allEntries = true)
    @Transactional
    public void deleteGallery(String carVersionId, String galleryId) {
        CarVersion carVersion = findCarVersion(carVersionId);
        CarGallery gallery = carGalleryRepository.findById(parseUuid(galleryId, "Id ảnh không hợp lệ."))
                .filter(item -> item.getCarVersion().getId().equals(carVersion.getId()))
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy ảnh."));
        carGalleryRepository.delete(gallery);
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
