package com.tayota.operationservice.service.car;

import com.tayota.operationservice.dto.response.car.CarSeriesWithVersionsResponseDTO;
import com.tayota.operationservice.dto.response.car.CarSpecificationResponseDTO;
import com.tayota.operationservice.dto.response.car.CarStyleWithVersionsResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionDetailResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionItemResponseDTO;
import com.tayota.operationservice.dto.response.car.PaginationResponseDTO;
import com.tayota.operationservice.entity.car.CarArticle;
import com.tayota.operationservice.entity.car.CarSeries;
import com.tayota.operationservice.entity.car.CarSpecification;
import com.tayota.operationservice.entity.car.CarStyle;
import com.tayota.operationservice.entity.car.CarVersion;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.mapper.car.CarCatalogMapper;
import com.tayota.operationservice.mapper.car.CarSpecificationMapper;
import com.tayota.operationservice.repository.car.CarAccessoryRepository;
import com.tayota.operationservice.repository.car.CarArticleRepository;
import com.tayota.operationservice.repository.car.CarGalleryRepository;
import com.tayota.operationservice.repository.car.CarPriceRepository;
import com.tayota.operationservice.repository.car.CarSeriesRepository;
import com.tayota.operationservice.repository.car.CarSpecificationRepository;
import com.tayota.operationservice.repository.car.CarStyleRepository;
import com.tayota.operationservice.repository.car.CarVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarCatalogService {
    private final CarStyleRepository carStyleRepository;
    private final CarSeriesRepository carSeriesRepository;
    private final CarVersionRepository carVersionRepository;
    private final CarSpecificationRepository carSpecificationRepository;
    private final CarPriceRepository carPriceRepository;
    private final CarGalleryRepository carGalleryRepository;
    private final CarArticleRepository carArticleRepository;
    private final CarAccessoryRepository carAccessoryRepository;
    private final CarSpecificationMapper carSpecificationMapper;
    private final CarCatalogMapper carCatalogMapper;

    // Lấy danh sách tất cả kiểu dáng kèm dòng xe và phiên bản xe từ cache catalog 2 giờ.
    @Cacheable(value = "catalogStylesWithVersions", key = "'all'")
    public List<CarStyleWithVersionsResponseDTO> getStylesWithVersions() {
        // Lấy toàn bộ phiên bản xe đang hiển thị để gom nhóm theo dòng xe.
        List<CarVersion> versions = carVersionRepository.findByVisibleTrue(
                Sort.by("modelYear").descending().and(Sort.by("name"))
        );

        // Lấy toàn bộ kiểu dáng và chuyển sang cấu trúc cây.
        return carStyleRepository.findAll(Sort.by("name"))
                .stream()
                .map(style -> mapStyleWithVersions(style, versions))
                .filter(style -> !style.getSeries().isEmpty())
                .toList();
    }

    // Lấy danh sách phiên bản xe theo điều kiện lọc.
    public PaginationResponseDTO<CarVersionItemResponseDTO> searchCarVersions(
            String keyword,
            String styleId,
            String seriesId,
            Integer modelYear,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size
    ) {
        // Tạo cấu hình phân trang và sắp xếp phiên bản mới nhất lên trước.
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizeSize(size),
                Sort.by("modelYear").descending().and(Sort.by("name"))
        );

        // Lọc danh sách phiên bản xe theo các điều kiện truyền vào.
        Page<CarVersion> result = carVersionRepository.search(
                toLikePattern(keyword),
                parseNullableUuid(styleId, "Id kiểu dáng không hợp lệ."),
                parseNullableUuid(seriesId, "Id dòng xe không hợp lệ."),
                modelYear,
                minPrice,
                maxPrice,
                pageable
        );

        // Chuyển danh sách phiên bản xe sang response phân trang.
        return new PaginationResponseDTO<>(
                result.getContent().stream().map(this::mapVersionItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // Lấy tất cả thông tin xe cụ thể từ cache chi tiết 1 giờ.
    @Cacheable(value = "catalogVersionDetail", key = "#carVersionId")
    public CarVersionDetailResponseDTO getCarVersionDetail(String carVersionId) {
        // Tìm phiên bản xe đang hiển thị cần xem chi tiết.
        CarVersion carVersion = findVisibleCarVersion(carVersionId);

        // Chuyển phiên bản xe sang response chi tiết.
        return mapVersionDetail(carVersion, false);
    }

    // Lấy chi tiết phiên bản xe cho màn hình quản lý, bao gồm bài viết ẩn.
    public CarVersionDetailResponseDTO getCarVersionDetailForManagement(String carVersionId) {
        // Tìm phiên bản xe bất kể trạng thái hiển thị.
        CarVersion carVersion = findCarVersion(carVersionId);

        // Chuyển phiên bản xe sang response chi tiết quản lý.
        return mapVersionDetail(carVersion, true);
    }

    // Lấy thông số kỹ thuật của xe từ cache thông số 6 giờ.
    @Cacheable(value = "catalogSpecification", key = "#carVersionId")
    public CarSpecificationResponseDTO getCarSpecification(String carVersionId) {
        // Kiểm tra phiên bản xe phải đang hiển thị trước khi trả thông số công khai.
        findVisibleCarVersion(carVersionId);

        // Chuyển id phiên bản xe sang UUID.
        UUID id = parseUuid(carVersionId, "Id phiên bản xe không hợp lệ.");

        // Lấy thông số kỹ thuật theo id phiên bản xe.
        CarSpecification specification = carSpecificationRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy thông số kỹ thuật của xe."));

        // Chuyển thông số kỹ thuật sang response.
        return carSpecificationMapper.toResponse(specification);
    }

    // So sánh xe theo danh sách phiên bản.
    public List<CarVersionDetailResponseDTO> compareCarVersions(List<String> carVersionIds) {
        // Kiểm tra số lượng phiên bản tối thiểu.
        if (carVersionIds == null || carVersionIds.isEmpty()) {
            throw new CustomException(400, "Cần chọn ít nhất 1 phiên bản xe để so sánh.");
        }

        // Kiểm tra số lượng phiên bản tối đa.
        if (carVersionIds.size() > 4) {
            throw new CustomException(400, "Chỉ có thể so sánh tối đa 4 phiên bản xe.");
        }

        // Lấy chi tiết từng phiên bản để hiển thị bảng so sánh.
        return carVersionIds.stream()
                .map(this::getCarVersionDetail)
                .toList();
    }

    // Chuyển kiểu dáng sang response kèm phiên bản.
    private CarStyleWithVersionsResponseDTO mapStyleWithVersions(CarStyle style, List<CarVersion> versions) {
        // Lấy các dòng xe thuộc kiểu dáng hiện tại.
        List<CarSeriesWithVersionsResponseDTO> series = carSeriesRepository.findByCarStyleId(style.getId())
                .stream()
                .map(item -> mapSeriesWithVersions(item, versions))
                .filter(item -> !item.getVersions().isEmpty())
                .toList();

        // Trả về kiểu dáng kèm danh sách dòng xe qua mapper object.
        return carCatalogMapper.toStyleWithVersions(style, series);
    }

    // Chuyển dòng xe sang response kèm phiên bản.
    private CarSeriesWithVersionsResponseDTO mapSeriesWithVersions(CarSeries series, List<CarVersion> versions) {
        // Lọc các phiên bản thuộc dòng xe hiện tại.
        List<CarVersionItemResponseDTO> items = versions.stream()
                .filter(version -> version.getCarSeries().getId().equals(series.getId()))
                .map(this::mapVersionItem)
                .toList();

        // Trả về dòng xe kèm danh sách phiên bản qua mapper object.
        return carCatalogMapper.toSeriesWithVersions(series, items);
    }

    // Chuyển phiên bản xe sang response danh sách.
    private CarVersionItemResponseDTO mapVersionItem(CarVersion carVersion) {
        // Lấy dữ liệu giá để mapper tính giá hiển thị thấp nhất.
        var prices = carPriceRepository.findByCarVersionId(carVersion.getId());

        // Lấy dữ liệu gallery để mapper chọn ảnh fallback khi xe chưa có ảnh đại diện.
        var galleries = carGalleryRepository.findByCarVersionId(carVersion.getId());

        // Lấy thông số kỹ thuật nếu phiên bản xe đã được cấu hình.
        CarSpecification specification = carSpecificationRepository.findById(carVersion.getId()).orElse(null);

        // Chuyển phiên bản xe sang response danh sách qua mapper object.
        return carCatalogMapper.toVersionItem(carVersion, prices, galleries, specification);
    }

    // Chuyển phiên bản xe sang response chi tiết.
    private CarVersionDetailResponseDTO mapVersionDetail(CarVersion carVersion, boolean includeHiddenArticles) {
        // Lấy id phiên bản xe để truy vấn các bảng liên quan.
        UUID id = carVersion.getId();

        // Lấy thông số kỹ thuật nếu đã được cấu hình.
        CarSpecification specification = carSpecificationRepository.findById(id).orElse(null);

        // Lấy danh sách giá theo từng tổ hợp màu.
        var prices = carPriceRepository.findByCarVersionId(id);

        // Lấy danh sách hình ảnh của phiên bản xe.
        var galleries = carGalleryRepository.findByCarVersionId(id);

        // Lấy danh sách bài viết giới thiệu phiên bản xe theo quyền xem dữ liệu ẩn.
        List<CarArticle> articles = includeHiddenArticles
                ? carArticleRepository.findByCarVersionId(id)
                : carArticleRepository.findByCarVersionIdAndPublishedTrue(id);

        // Lấy danh sách phụ kiện tương thích với phiên bản xe.
        var accessories = carAccessoryRepository.findByCarVersionId(id);

        // Chuyển dữ liệu chi tiết sang response qua mapper object.
        return carCatalogMapper.toVersionDetail(carVersion, specification, prices, galleries, articles, accessories);
    }

    // Tìm phiên bản xe theo id.
    private CarVersion findCarVersion(String carVersionId) {
        // Chuyển id phiên bản xe sang UUID.
        UUID id = parseUuid(carVersionId, "Id phiên bản xe không hợp lệ.");

        // Lấy phiên bản xe từ cơ sở dữ liệu.
        return carVersionRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiên bản xe."));
    }

    // Tìm phiên bản xe công khai theo id.
    private CarVersion findVisibleCarVersion(String carVersionId) {
        // Lấy phiên bản xe theo id.
        CarVersion carVersion = findCarVersion(carVersionId);

        // Chặn phiên bản xe đã bị ẩn khỏi catalog công khai.
        if (!carVersion.isVisible()) {
            throw new CustomException(404, "Không tìm thấy phiên bản xe.");
        }

        // Trả về phiên bản xe đang hiển thị.
        return carVersion;
    }

    // Chuyển chuỗi id sang UUID.
    private UUID parseUuid(String value, String message) {
        try {
            // Chuyển chuỗi hợp lệ sang UUID.
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            // Trả lỗi nếu chuỗi không đúng định dạng UUID.
            throw new CustomException(400, message);
        }
    }

    // Chuyển chuỗi id có thể null sang UUID.
    private UUID parseNullableUuid(String value, String message) {
        // Bỏ qua điều kiện lọc nếu id không được truyền vào.
        if (!StringUtils.hasText(value)) {
            return null;
        }

        // Chuyển id sang UUID khi có dữ liệu.
        return parseUuid(value, message);
    }

    // Chuẩn hóa keyword rỗng thành null.
    private String normalizeText(String value) {
        // Trả null nếu chuỗi không có nội dung.
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // Chuyển keyword thành pattern tìm kiếm SQL LIKE.
    private String toLikePattern(String value) {
        // Chuẩn hóa keyword trước khi đưa vào query.
        String normalized = normalizeText(value);

        // Trả pattern tất cả nếu không có keyword.
        return normalized == null ? "%" : "%" + normalized.toLowerCase() + "%";
    }

    // Chuẩn hóa kích thước trang.
    private int normalizeSize(int size) {
        // Dùng kích thước mặc định nếu request truyền giá trị không hợp lệ.
        if (size <= 0) {
            return 20;
        }

        // Giới hạn kích thước trang để tránh truy vấn quá lớn.
        return Math.min(size, 50);
    }
}
