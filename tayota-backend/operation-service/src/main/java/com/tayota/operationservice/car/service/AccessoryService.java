package com.tayota.operationservice.car.service;

import com.tayota.operationservice.car.dto.Request.AccessoryRequestDTO;
import com.tayota.operationservice.car.dto.Request.CarAccessoryRequestDTO;
import com.tayota.operationservice.car.dto.Response.AccessoryResponseDTO;
import com.tayota.operationservice.car.dto.Response.PaginationResponseDTO;
import com.tayota.operationservice.car.entity.Accessory;
import com.tayota.operationservice.car.entity.CarAccessory;
import com.tayota.operationservice.car.entity.CarAccessoryId;
import com.tayota.operationservice.car.entity.CarVersion;
import com.tayota.operationservice.car.mapper.AccessoryMapper;
import com.tayota.operationservice.car.repository.AccessoryRepository;
import com.tayota.operationservice.car.repository.CarAccessoryRepository;
import com.tayota.operationservice.car.repository.CarVersionRepository;
import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessoryService {
    private final AccessoryRepository accessoryRepository;
    private final CarVersionRepository carVersionRepository;
    private final CarAccessoryRepository carAccessoryRepository;
    private final AccessoryMapper accessoryMapper;

    // Lấy tất cả phụ kiện theo điều kiện lọc
    @Cacheable(
            value = "accessorySearch",
            key = "{#keyword, #type, #seriesId, #versionId, #page, #size}"
    )
    public PaginationResponseDTO<AccessoryResponseDTO> searchAccessories(
            String keyword,
            String type,
            String seriesId,
            String versionId,
            int page,
            int size
    ) {
        // Tạo cấu hình phân trang mặc định 20 phụ kiện mỗi trang
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size), Sort.by("type").and(Sort.by("model")));

        // Lọc phụ kiện theo keyword, loại, dòng xe hoặc phiên bản xe
        Page<Accessory> result = accessoryRepository.search(
                normalizeText(keyword),
                normalizeText(type),
                parseNullableUuid(seriesId, "Id dòng xe không hợp lệ."),
                parseNullableUuid(versionId, "Id phiên bản xe không hợp lệ."),
                pageable
        );

        // Chuyển danh sách phụ kiện sang response phân trang
        return new PaginationResponseDTO<>(
                result.getContent().stream().map(accessoryMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // Lấy phụ kiện theo id
    @Cacheable(value = "accessoryDetail", key = "#accessoryId")
    public AccessoryResponseDTO getAccessory(String accessoryId) {
        // Tìm phụ kiện và chuyển sang response
        return accessoryMapper.toResponse(findAccessory(accessoryId));
    }

    // Thêm phụ kiện
    @CacheEvict(value = {"accessorySearch", "catalogVersionDetail"}, allEntries = true)
    @Transactional
    public AccessoryResponseDTO createAccessory(AccessoryRequestDTO requestDTO) {
        // Tạo phụ kiện mới từ request
        Accessory accessory = Accessory.builder()
                .model(requestDTO.getModel())
                .brand(requestDTO.getBrand())
                .price(requestDTO.getPrice())
                .description(requestDTO.getDescription())
                .useContent(requestDTO.getUseContent())
                .reminderContent(requestDTO.getReminderContent())
                .type(requestDTO.getType())
                .build();

        // Lưu phụ kiện vào csdl
        return accessoryMapper.toResponse(accessoryRepository.save(accessory));
    }

    // Cập nhật phụ kiện
    @CacheEvict(value = {"accessorySearch", "accessoryDetail", "catalogVersionDetail"}, allEntries = true)
    @Transactional
    public AccessoryResponseDTO updateAccessory(String accessoryId, AccessoryRequestDTO requestDTO) {
        // Tìm phụ kiện cần cập nhật
        Accessory accessory = findAccessory(accessoryId);

        // Cập nhật thông tin phụ kiện
        accessory.setModel(requestDTO.getModel());
        accessory.setBrand(requestDTO.getBrand());
        accessory.setPrice(requestDTO.getPrice());
        accessory.setDescription(requestDTO.getDescription());
        accessory.setUseContent(requestDTO.getUseContent());
        accessory.setReminderContent(requestDTO.getReminderContent());
        accessory.setType(requestDTO.getType());

        // Lưu phụ kiện sau khi cập nhật
        return accessoryMapper.toResponse(accessoryRepository.save(accessory));
    }

    // Xóa phụ kiện
    @CacheEvict(value = {"accessorySearch", "accessoryDetail", "catalogVersionDetail"}, allEntries = true)
    @Transactional
    public void deleteAccessory(String accessoryId) {
        // Xóa phụ kiện theo id
        accessoryRepository.delete(findAccessory(accessoryId));
    }

    // Gắn phụ kiện cho phiên bản xe
    @CacheEvict(value = {"accessorySearch", "catalogVersionDetail"}, allEntries = true)
    @Transactional
    public void attachAccessoryToVersion(CarAccessoryRequestDTO requestDTO) {
        // Tìm phiên bản xe cần gắn phụ kiện
        CarVersion carVersion = findCarVersion(requestDTO.getCarVersionId());

        // Tìm phụ kiện cần gắn
        Accessory accessory = findAccessory(requestDTO.getAccessoryId());

        // Tạo khóa chính cho liên kết phiên bản xe và phụ kiện
        CarAccessoryId id = new CarAccessoryId(carVersion.getId(), accessory.getId());

        // Bỏ qua nếu liên kết đã tồn tại
        if (carAccessoryRepository.existsById(id)) {
            return;
        }

        // Lưu liên kết phụ kiện và phiên bản xe
        carAccessoryRepository.save(CarAccessory.builder()
                .id(id)
                .carVersion(carVersion)
                .accessory(accessory)
                .build());
    }

    // Gỡ phụ kiện khỏi phiên bản xe
    @CacheEvict(value = {"accessorySearch", "catalogVersionDetail"}, allEntries = true)
    @Transactional
    public void detachAccessoryFromVersion(CarAccessoryRequestDTO requestDTO) {
        // Tìm phiên bản xe cần gỡ phụ kiện
        CarVersion carVersion = findCarVersion(requestDTO.getCarVersionId());

        // Tìm phụ kiện cần gỡ
        Accessory accessory = findAccessory(requestDTO.getAccessoryId());

        // Xóa liên kết giữa phụ kiện và phiên bản xe
        carAccessoryRepository.deleteById(new CarAccessoryId(carVersion.getId(), accessory.getId()));
    }

    // Tìm phụ kiện theo id
    private Accessory findAccessory(String accessoryId) {
        // Chuyển id phụ kiện sang UUID
        UUID id = parseUuid(accessoryId, "Id phụ kiện không hợp lệ.");

        // Lấy phụ kiện từ csdl
        return accessoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phụ kiện."));
    }

    // Tìm phiên bản xe theo id
    private CarVersion findCarVersion(String carVersionId) {
        // Chuyển id phiên bản xe sang UUID
        UUID id = parseUuid(carVersionId, "Id phiên bản xe không hợp lệ.");

        // Lấy phiên bản xe từ csdl
        return carVersionRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiên bản xe."));
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

    // Chuẩn hóa keyword rỗng thành null
    private String normalizeText(String value) {
        // Trả null nếu chuỗi không có nội dung
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // Chuẩn hóa kích thước trang phụ kiện
    private int normalizeSize(int size) {
        // Dùng kích thước mặc định nếu request truyền giá trị không hợp lệ
        if (size <= 0) {
            return 20;
        }

        // Giới hạn kích thước trang để tránh truy vấn quá lớn
        return Math.min(size, 50);
    }
}
