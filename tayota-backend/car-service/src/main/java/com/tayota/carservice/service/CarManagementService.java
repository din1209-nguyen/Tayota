package com.tayota.carservice.service;

import com.tayota.carservice.dto.Request.CarRequestDTO;
import com.tayota.carservice.dto.Response.CarResponseDTO;
import com.tayota.carservice.dto.Response.PaginationResponseDTO;
import com.tayota.carservice.entity.Car;
import com.tayota.carservice.entity.CarVersion;
import com.tayota.carservice.entity.Dealership;
import com.tayota.carservice.enums.CarStatusType;
import com.tayota.carservice.mapper.CarMapper;
import com.tayota.carservice.repository.CarRepository;
import com.tayota.carservice.repository.CarVersionRepository;
import com.tayota.carservice.repository.DealershipRepository;
import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarManagementService {
    private final CarRepository carRepository;
    private final CarVersionRepository carVersionRepository;
    private final DealershipRepository dealershipRepository;
    private final CarMapper carMapper;

    // Lấy danh sách xe vật lý theo điều kiện lọc
    @Cacheable(
            value = "physicalCarSearch",
            key = "{#carVersionId, #dealershipId, #ownerUserId, #status, #page, #size}"
    )
    public PaginationResponseDTO<CarResponseDTO> searchCars(
            String carVersionId,
            String dealershipId,
            String ownerUserId,
            CarStatusType status,
            int page,
            int size
    ) {
        // Tạo cấu hình phân trang cho danh sách xe vật lý
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size));

        // Lọc xe vật lý theo phiên bản, đại lý, chủ sở hữu và trạng thái
        Page<Car> result = carRepository.findAll(buildSpecification(carVersionId, dealershipId, ownerUserId, status), pageable);

        // Chuyển danh sách xe vật lý sang response phân trang
        return new PaginationResponseDTO<>(
                result.getContent().stream().map(carMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // Lấy các xe sở hữu theo userId
    public List<CarResponseDTO> getCarsByUserId(String userId) {
        // Chuyển userId sang UUID
        UUID ownerUserId = parseUuid(userId, "Id người dùng không hợp lệ.");

        // Lấy danh sách xe theo chủ sở hữu
        return carRepository.findByOwnerUserId(ownerUserId)
                .stream()
                .map(carMapper::toResponse)
                .toList();
    }

    // Lấy xe vật lý theo VIN
    public CarResponseDTO getCar(String vinId) {
        // Tìm xe vật lý và chuyển sang response
        return carMapper.toResponse(findCar(vinId));
    }

    // Thêm xe vật lý
    @CacheEvict(value = "physicalCarSearch", allEntries = true)
    @Transactional
    public CarResponseDTO createCar(CarRequestDTO requestDTO) {
        // Kiểm tra số VIN chưa tồn tại
        if (carRepository.existsById(requestDTO.getVinId())) {
            throw new CustomException(409, "Số VIN đã tồn tại.");
        }

        // Kiểm tra số máy chưa tồn tại
        if (carRepository.existsByEngineNumber(requestDTO.getEngineNumber())) {
            throw new CustomException(409, "Số máy đã tồn tại.");
        }

        // Tạo xe vật lý mới
        Car car = Car.builder()
                .vinId(requestDTO.getVinId())
                .carVersion(findCarVersion(requestDTO.getCarVersionId()))
                .dealership(findDealership(requestDTO.getDealershipId()))
                .engineNumber(requestDTO.getEngineNumber())
                .ownerUserId(parseNullableUuid(requestDTO.getOwnerUserId(), "Id người dùng không hợp lệ."))
                .status(requestDTO.getStatus())
                .productedYear(requestDTO.getProductedYear())
                .build();

        // Lưu xe vật lý vào csdl
        return carMapper.toResponse(carRepository.save(car));
    }

    // Cập nhật xe vật lý
    @CacheEvict(value = "physicalCarSearch", allEntries = true)
    @Transactional
    public CarResponseDTO updateCar(String vinId, CarRequestDTO requestDTO) {
        // Tìm xe vật lý cần cập nhật
        Car car = findCar(vinId);

        // Kiểm tra số máy chưa bị xe khác sử dụng
        if (carRepository.existsByEngineNumberAndVinIdNot(requestDTO.getEngineNumber(), vinId)) {
            throw new CustomException(409, "Số máy đã tồn tại.");
        }

        // Cập nhật thông tin xe vật lý
        car.setCarVersion(findCarVersion(requestDTO.getCarVersionId()));
        car.setDealership(findDealership(requestDTO.getDealershipId()));
        car.setEngineNumber(requestDTO.getEngineNumber());
        car.setOwnerUserId(parseNullableUuid(requestDTO.getOwnerUserId(), "Id người dùng không hợp lệ."));
        car.setStatus(requestDTO.getStatus());
        car.setProductedYear(requestDTO.getProductedYear());

        // Lưu xe vật lý sau khi cập nhật
        return carMapper.toResponse(carRepository.save(car));
    }

    // Xóa xe vật lý
    @CacheEvict(value = "physicalCarSearch", allEntries = true)
    @Transactional
    public void deleteCar(String vinId) {
        // Xóa xe vật lý theo VIN
        carRepository.delete(findCar(vinId));
    }

    // Tạo điều kiện lọc xe vật lý
    private Specification<Car> buildSpecification(String carVersionId, String dealershipId, String ownerUserId, CarStatusType status) {
        return (root, query, criteriaBuilder) -> {
            // Khởi tạo điều kiện lọc rỗng
            var predicates = criteriaBuilder.conjunction();

            // Chuyển các id lọc sang UUID nếu được truyền vào
            UUID versionId = parseNullableUuid(carVersionId, "Id phiên bản xe không hợp lệ.");
            UUID dealerId = parseNullableUuid(dealershipId, "Id đại lý không hợp lệ.");
            UUID userId = parseNullableUuid(ownerUserId, "Id người dùng không hợp lệ.");

            // Thêm điều kiện lọc theo phiên bản xe
            if (versionId != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("carVersion").get("id"), versionId));
            }

            // Thêm điều kiện lọc theo đại lý
            if (dealerId != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("dealership").get("id"), dealerId));
            }

            // Thêm điều kiện lọc theo chủ sở hữu
            if (userId != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("ownerUserId"), userId));
            }

            // Thêm điều kiện lọc theo trạng thái xe
            if (status != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("status"), status));
            }

            // Trả về điều kiện lọc hoàn chỉnh
            return predicates;
        };
    }

    // Tìm xe vật lý theo VIN
    private Car findCar(String vinId) {
        // Lấy xe vật lý từ csdl
        return carRepository.findById(vinId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy xe vật lý."));
    }

    // Tìm phiên bản xe theo id
    private CarVersion findCarVersion(String carVersionId) {
        // Chuyển id phiên bản xe sang UUID
        UUID id = parseUuid(carVersionId, "Id phiên bản xe không hợp lệ.");

        // Lấy phiên bản xe từ csdl
        return carVersionRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiên bản xe."));
    }

    // Tìm đại lý theo id
    private Dealership findDealership(String dealershipId) {
        // Chuyển id đại lý sang UUID
        UUID id = parseUuid(dealershipId, "Id đại lý không hợp lệ.");

        // Lấy đại lý từ csdl
        return dealershipRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy đại lý."));
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
        if (!StringUtils.hasText(value)) {
            return null;
        }

        // Chuyển id sang UUID khi có dữ liệu
        return parseUuid(value, message);
    }

    // Chuẩn hóa kích thước trang
    private int normalizeSize(int size) {
        // Dùng kích thước mặc định nếu request truyền giá trị không hợp lệ
        if (size <= 0) {
            return 20;
        }

        // Giới hạn kích thước trang để tránh truy vấn quá lớn
        return Math.min(size, 100);
    }
}
