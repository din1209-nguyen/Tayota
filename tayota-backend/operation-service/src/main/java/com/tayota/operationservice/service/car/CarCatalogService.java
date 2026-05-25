package com.tayota.operationservice.service.car;

import com.tayota.operationservice.dto.response.car.AccessoryResponseDTO;
import com.tayota.operationservice.dto.response.car.CarArticleResponseDTO;
import com.tayota.operationservice.dto.response.car.CarGalleryResponseDTO;
import com.tayota.operationservice.dto.response.car.CarPriceResponseDTO;
import com.tayota.operationservice.dto.response.car.CarSeriesWithVersionsResponseDTO;
import com.tayota.operationservice.dto.response.car.CarSpecificationResponseDTO;
import com.tayota.operationservice.dto.response.car.CarStyleWithVersionsResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionDetailResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionItemResponseDTO;
import com.tayota.operationservice.dto.response.car.PaginationResponseDTO;
import com.tayota.operationservice.entity.car.CarGallery;
import com.tayota.operationservice.entity.car.CarPrice;
import com.tayota.operationservice.entity.car.CarSeries;
import com.tayota.operationservice.entity.car.CarSpecification;
import com.tayota.operationservice.entity.car.CarStyle;
import com.tayota.operationservice.entity.car.CarVersion;
import com.tayota.operationservice.mapper.car.AccessoryMapper;
import com.tayota.operationservice.mapper.car.CarArticleMapper;
import com.tayota.operationservice.mapper.car.CarGalleryMapper;
import com.tayota.operationservice.mapper.car.CarPriceMapper;
import com.tayota.operationservice.mapper.car.CarSeriesMapper;
import com.tayota.operationservice.mapper.car.CarSpecificationMapper;
import com.tayota.operationservice.mapper.car.CarStyleMapper;
import com.tayota.operationservice.mapper.car.CarVersionMapper;
import com.tayota.operationservice.repository.car.CarAccessoryRepository;
import com.tayota.operationservice.repository.car.CarArticleRepository;
import com.tayota.operationservice.repository.car.CarGalleryRepository;
import com.tayota.operationservice.repository.car.CarPriceRepository;
import com.tayota.operationservice.repository.car.CarSeriesRepository;
import com.tayota.operationservice.repository.car.CarSpecificationRepository;
import com.tayota.operationservice.repository.car.CarStyleRepository;
import com.tayota.operationservice.repository.car.CarVersionRepository;
import com.tayota.operationservice.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
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
    private final AccessoryMapper accessoryMapper;
    private final CarArticleMapper carArticleMapper;
    private final CarGalleryMapper carGalleryMapper;
    private final CarPriceMapper carPriceMapper;
    private final CarSeriesMapper carSeriesMapper;
    private final CarSpecificationMapper carSpecificationMapper;
    private final CarStyleMapper carStyleMapper;
    private final CarVersionMapper carVersionMapper;

    // Lấy danh sách tất cả kiểu dáng kèm dòng xe và phiên bản xe.
    @Cacheable(value = "catalogStylesWithVersions", key = "'all'")
    public List<CarStyleWithVersionsResponseDTO> getStylesWithVersions() {
        // Lấy toàn bộ phiên bản xe đang hiển thị để gom nhóm theo dòng xe.
        List<CarVersion> versions = carVersionRepository.findByVisibleTrue(Sort.by("modelYear").descending().and(Sort.by("name")));

        // Lấy toàn bộ kiểu dáng và chuyển sang cấu trúc cây.
        return carStyleRepository.findAll(Sort.by("name"))
                .stream()
                .map(style -> toStyleWithVersions(style, versions))
                .filter(style -> !style.getSeries().isEmpty())
                .toList();
    }

    // Lấy danh sách phiên bản xe theo điều kiện lọc
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
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size), Sort.by("modelYear").descending().and(Sort.by("name")));

        // Lọc danh sách phiên bản xe theo các điều kiện truyền vào
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
                result.getContent().stream().map(this::mapToVersionItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // Lấy tất cả thông tin xe cụ thể.
    @Cacheable(value = "catalogVersionDetail", key = "#carVersionId")
    public CarVersionDetailResponseDTO getCarVersionDetail(String carVersionId) {
        // Tìm phiên bản xe cần xem chi tiết.
        CarVersion carVersion = findVisibleCarVersion(carVersionId);

        // Chuyển phiên bản xe sang response chi tiết.
        return toVersionDetail(carVersion);
    }

    // Lấy thông số kỹ thuật của xe.
    @Cacheable(value = "catalogSpecification", key = "#carVersionId")
    public CarSpecificationResponseDTO getCarSpecification(String carVersionId) {
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
    private CarStyleWithVersionsResponseDTO toStyleWithVersions(CarStyle style, List<CarVersion> versions) {
        // Lấy các dòng xe thuộc kiểu dáng hiện tại.
        List<CarSeriesWithVersionsResponseDTO> series = carSeriesRepository.findByCarStyleId(style.getId())
                .stream()
                .map(item -> toSeriesWithVersions(item, versions))
                .filter(item -> !item.getVersions().isEmpty())
                .toList();

        // Trả về kiểu dáng kèm danh sách dòng xe.
        return carStyleMapper.toWithVersions(style, series);
    }

    // Chuyển dòng xe sang response kèm phiên bản.
    private CarSeriesWithVersionsResponseDTO toSeriesWithVersions(CarSeries series, List<CarVersion> versions) {
        // Lọc các phiên bản thuộc dòng xe hiện tại.
        List<CarVersionItemResponseDTO> items = versions.stream()
                .filter(version -> version.getCarSeries().getId().equals(series.getId()))
                .map(this::mapToVersionItem)
                .toList();

        // Trả về dòng xe kèm danh sách phiên bản.
        return carSeriesMapper.toWithVersions(series, items);
    }

    // Chuyển phiên bản xe sang response danh sách.
    private CarVersionItemResponseDTO mapToVersionItem(CarVersion carVersion) {
        // Lấy danh sách giá theo màu nội thất và ngoại thất.
        List<CarPrice> prices = carPriceRepository.findByCarVersionId(carVersion.getId());

        // Lấy giá thấp nhất để hiển thị ở danh sách.
        BigDecimal minPrice = prices.stream()
                .map(CarPrice::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);

        // Lấy ảnh đại diện từ bảng giá, nếu không có thì lấy từ gallery.
        String imageUrl = prices.stream()
                .map(CarPrice::getExImageUrl)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseGet(() -> carGalleryRepository.findByCarVersionId(carVersion.getId()).stream()
                        .map(CarGallery::getImageUrl)
                        .findFirst()
                        .orElse(null));

        // Trả về response rút gọn cho danh sách phiên bản xe
        CarSpecificationResponseDTO specification = carSpecificationRepository.findById(carVersion.getId())
                .map(carSpecificationMapper::toResponse)
                .orElse(null);
        return carVersionMapper.toItem(carVersion, minPrice, imageUrl, specification);
    }

    // Chuyển phiên bản xe sang response chi tiết.
    private CarVersionDetailResponseDTO toVersionDetail(CarVersion carVersion) {
        // Lấy id phiên bản xe để truy vấn các bảng liên quan.
        UUID id = carVersion.getId();

        // Lấy thông số kỹ thuật nếu đã được cấu hình.
        CarSpecificationResponseDTO specification = carSpecificationRepository.findById(id)
                .map(carSpecificationMapper::toResponse)
                .orElse(null);

        // Lấy danh sách giá theo từng tổ hợp màu.
        List<CarPriceResponseDTO> prices = carPriceRepository.findByCarVersionId(id)
                .stream()
                .map(carPriceMapper::toResponse)
                .toList();

        // Lấy danh sách hình ảnh của phiên bản xe.
        List<CarGalleryResponseDTO> galleries = carGalleryRepository.findByCarVersionId(id)
                .stream()
                .map(carGalleryMapper::toResponse)
                .toList();

        // Lấy danh sách bài viết giới thiệu phiên bản xe.
        List<CarArticleResponseDTO> articles = carArticleRepository.findByCarVersionIdAndPublishedTrue(id)
                .stream()
                .map(carArticleMapper::toResponse)
                .toList();

        // Lấy danh sách phụ kiện tương thích với phiên bản xe.
        List<AccessoryResponseDTO> accessories = carAccessoryRepository.findByCarVersionId(id)
                .stream()
                .filter(carAccessory -> carAccessory.getAccessory().isVisible())
                .map(carAccessory -> accessoryMapper.toResponse(carAccessory.getAccessory()))
                .toList();

        // Trả về toàn bộ thông tin chi tiết cho trang giới thiệu xe.
        return carVersionMapper.toDetail(
                carVersion,
                carSeriesMapper.toResponse(carVersion.getCarSeries()),
                specification,
                prices,
                galleries,
                articles,
                accessories
        );
    }

    // Tao dieu kien loc phien ban xe theo cac tham so duoc truyen vao
    private Specification<CarVersion> buildSpecification(
            String keyword,
            String styleId,
            String seriesId,
            Integer modelYear,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();
            String normalizedKeyword = normalizeText(keyword);
            UUID parsedStyleId = parseNullableUuid(styleId, "Id kiểu dáng không hợp lệ.");
            UUID parsedSeriesId = parseNullableUuid(seriesId, "Id dòng xe không hợp lệ.");

            if (normalizedKeyword != null) {
                String keywordPattern = "%" + normalizedKeyword.toLowerCase() + "%";
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordPattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("carSeries").get("name")), keywordPattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("carSeries").get("carStyle").get("name")), keywordPattern)
                        )
                );
            }

            if (parsedStyleId != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("carSeries").get("carStyle").get("id"), parsedStyleId)
                );
            }

            if (parsedSeriesId != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("carSeries").get("id"), parsedSeriesId)
                );
            }

            if (modelYear != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("modelYear"), modelYear));
            }

            if (minPrice != null || maxPrice != null) {
                var priceSubquery = query.subquery(Integer.class);
                var priceRoot = priceSubquery.from(CarPrice.class);
                var pricePredicates = criteriaBuilder.equal(priceRoot.get("carVersion"), root);

                if (minPrice != null) {
                    pricePredicates = criteriaBuilder.and(
                            pricePredicates,
                            criteriaBuilder.greaterThanOrEqualTo(priceRoot.get("price"), minPrice)
                    );
                }

                if (maxPrice != null) {
                    pricePredicates = criteriaBuilder.and(
                            pricePredicates,
                            criteriaBuilder.lessThanOrEqualTo(priceRoot.get("price"), maxPrice)
                    );
                }

                priceSubquery.select(criteriaBuilder.literal(1)).where(pricePredicates);
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.exists(priceSubquery));
            }

            return predicates;
        };
    }

    // Tìm phiên bản xe theo id.
    private CarVersion findCarVersion(String carVersionId) {
        // Chuyển id phiên bản xe sang UUID.
        UUID id = parseUuid(carVersionId, "Id phiên bản xe không hợp lệ.");

        // Lấy phiên bản xe từ cơ sở dữ liệu.
        return carVersionRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiên bản xe."));
    }

    private CarVersion findVisibleCarVersion(String carVersionId) {
        CarVersion carVersion = findCarVersion(carVersionId);
        if (!carVersion.isVisible()) {
            throw new CustomException(404, "Không tìm thấy phiên bản xe.");
        }
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

    private String toLikePattern(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? "%" : "%" + normalized.toLowerCase() + "%";
    }

    // Chuẩn hóa kích thước trang
    private int normalizeSize(int size) {
        // Dùng kích thước mặc định nếu request truyền giá trị không hợp lệ.
        if (size <= 0) {
            return 20;
        }

        // Giới hạn kích thước trang để tránh truy vấn quá lớn.
        return Math.min(size, 50);
    }
}
