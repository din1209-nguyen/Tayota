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

    // Láº¥y danh sÃ¡ch táº¥t cáº£ kiá»ƒu dÃ¡ng kÃ¨m dÃ²ng xe vÃ  phiÃªn báº£n xe
    @Cacheable(value = "catalogStylesWithVersions", key = "'all'")
    public List<CarStyleWithVersionsResponseDTO> getStylesWithVersions() {
        // Láº¥y toÃ n bá»™ phiÃªn báº£n xe Ä‘á»ƒ gom nhÃ³m theo dÃ²ng xe
        List<CarVersion> versions = carVersionRepository.findAll(Sort.by("modelYear").descending().and(Sort.by("name")));

        // Láº¥y toÃ n bá»™ kiá»ƒu dÃ¡ng vÃ  chuyá»ƒn sang cáº¥u trÃºc cÃ¢y
        return carStyleRepository.findAll(Sort.by("name"))
                .stream()
                .map(style -> toStyleWithVersions(style, versions))
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
        // Táº¡o cáº¥u hÃ¬nh phÃ¢n trang vÃ  sáº¯p xáº¿p phiÃªn báº£n má»›i nháº¥t lÃªn trÆ°á»›c
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

        // Chuyá»ƒn danh sÃ¡ch phiÃªn báº£n xe sang response phÃ¢n trang
        return new PaginationResponseDTO<>(
                result.getContent().stream().map(this::mapToVersionItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // Láº¥y táº¥t cáº£ thÃ´ng tin xe cá»¥ thá»ƒ
    @Cacheable(value = "catalogVersionDetail", key = "#carVersionId")
    public CarVersionDetailResponseDTO getCarVersionDetail(String carVersionId) {
        // TÃ¬m phiÃªn báº£n xe cáº§n xem chi tiáº¿t
        CarVersion carVersion = findCarVersion(carVersionId);

        // Chuyá»ƒn phiÃªn báº£n xe sang response chi tiáº¿t
        return toVersionDetail(carVersion);
    }

    // Láº¥y thÃ´ng sá»‘ ká»¹ thuáº­t cá»§a xe
    @Cacheable(value = "catalogSpecification", key = "#carVersionId")
    public CarSpecificationResponseDTO getCarSpecification(String carVersionId) {
        // Chuyá»ƒn id phiÃªn báº£n xe sang UUID
        UUID id = parseUuid(carVersionId, "Id phiÃªn báº£n xe khÃ´ng há»£p lá»‡.");

        // Láº¥y thÃ´ng sá»‘ ká»¹ thuáº­t theo id phiÃªn báº£n xe
        CarSpecification specification = carSpecificationRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "KhÃ´ng tÃ¬m tháº¥y thÃ´ng sá»‘ ká»¹ thuáº­t cá»§a xe."));

        // Chuyá»ƒn thÃ´ng sá»‘ ká»¹ thuáº­t sang response
        return carSpecificationMapper.toResponse(specification);
    }

    // So sÃ¡nh xe theo danh sÃ¡ch phiÃªn báº£n
    public List<CarVersionDetailResponseDTO> compareCarVersions(List<String> carVersionIds) {
        // Kiá»ƒm tra sá»‘ lÆ°á»£ng phiÃªn báº£n tá»‘i thiá»ƒu
        if (carVersionIds == null || carVersionIds.isEmpty()) {
            throw new CustomException(400, "Cần chọn ít nhất 1 phiên bản xe để so sánh.");
        }

        // Kiá»ƒm tra sá»‘ lÆ°á»£ng phiÃªn báº£n tá»‘i Ä‘a
        if (carVersionIds.size() > 4) {
            throw new CustomException(400, "Chỉ có thể so sánh tối đa 4 phiên bản xe.");
        }

        // Láº¥y chi tiáº¿t tá»«ng phiÃªn báº£n Ä‘á»ƒ hiá»ƒn thá»‹ báº£ng so sÃ¡nh
        return carVersionIds.stream()
                .map(this::getCarVersionDetail)
                .toList();
    }

    // Chuyá»ƒn kiá»ƒu dÃ¡ng sang response kÃ¨m phiÃªn báº£n
    private CarStyleWithVersionsResponseDTO toStyleWithVersions(CarStyle style, List<CarVersion> versions) {
        // Láº¥y cÃ¡c dÃ²ng xe thuá»™c kiá»ƒu dÃ¡ng hiá»‡n táº¡i
        List<CarSeriesWithVersionsResponseDTO> series = carSeriesRepository.findByCarStyleId(style.getId())
                .stream()
                .map(item -> toSeriesWithVersions(item, versions))
                .toList();

        // Tráº£ vá» kiá»ƒu dÃ¡ng kÃ¨m danh sÃ¡ch dÃ²ng xe
        return carStyleMapper.toWithVersions(style, series);
    }

    // Chuyá»ƒn dÃ²ng xe sang response kÃ¨m phiÃªn báº£n
    private CarSeriesWithVersionsResponseDTO toSeriesWithVersions(CarSeries series, List<CarVersion> versions) {
        // Lá»c cÃ¡c phiÃªn báº£n thuá»™c dÃ²ng xe hiá»‡n táº¡i
        List<CarVersionItemResponseDTO> items = versions.stream()
                .filter(version -> version.getCarSeries().getId().equals(series.getId()))
                .map(this::mapToVersionItem)
                .toList();

        // Tráº£ vá» dÃ²ng xe kÃ¨m danh sÃ¡ch phiÃªn báº£n
        return carSeriesMapper.toWithVersions(series, items);
    }

    // Chuyá»ƒn phiÃªn báº£n xe sang response danh sÃ¡ch
    private CarVersionItemResponseDTO mapToVersionItem(CarVersion carVersion) {
        // Láº¥y danh sÃ¡ch giÃ¡ theo mÃ u ná»™i tháº¥t vÃ  ngoáº¡i tháº¥t
        List<CarPrice> prices = carPriceRepository.findByCarVersionId(carVersion.getId());

        // Láº¥y giÃ¡ tháº¥p nháº¥t Ä‘á»ƒ hiá»ƒn thá»‹ á»Ÿ danh sÃ¡ch
        BigDecimal minPrice = prices.stream()
                .map(CarPrice::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);

        // Láº¥y áº£nh Ä‘áº¡i diá»‡n tá»« báº£ng giÃ¡, náº¿u khÃ´ng cÃ³ thÃ¬ láº¥y tá»« gallery
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

    // Chuyá»ƒn phiÃªn báº£n xe sang response chi tiáº¿t
    private CarVersionDetailResponseDTO toVersionDetail(CarVersion carVersion) {
        // Láº¥y id phiÃªn báº£n xe Ä‘á»ƒ truy váº¥n cÃ¡c báº£ng liÃªn quan
        UUID id = carVersion.getId();

        // Láº¥y thÃ´ng sá»‘ ká»¹ thuáº­t náº¿u Ä‘Ã£ Ä‘Æ°á»£c cáº¥u hÃ¬nh
        CarSpecificationResponseDTO specification = carSpecificationRepository.findById(id)
                .map(carSpecificationMapper::toResponse)
                .orElse(null);

        // Láº¥y danh sÃ¡ch giÃ¡ theo tá»«ng tá»• há»£p mÃ u
        List<CarPriceResponseDTO> prices = carPriceRepository.findByCarVersionId(id)
                .stream()
                .map(carPriceMapper::toResponse)
                .toList();

        // Láº¥y danh sÃ¡ch hÃ¬nh áº£nh cá»§a phiÃªn báº£n xe
        List<CarGalleryResponseDTO> galleries = carGalleryRepository.findByCarVersionId(id)
                .stream()
                .map(carGalleryMapper::toResponse)
                .toList();

        // Láº¥y danh sÃ¡ch bÃ i viáº¿t giá»›i thiá»‡u phiÃªn báº£n xe
        List<CarArticleResponseDTO> articles = carArticleRepository.findByCarVersionId(id)
                .stream()
                .map(carArticleMapper::toResponse)
                .toList();

        // Láº¥y danh sÃ¡ch phá»¥ kiá»‡n tÆ°Æ¡ng thÃ­ch vá»›i phiÃªn báº£n xe
        List<AccessoryResponseDTO> accessories = carAccessoryRepository.findByCarVersionId(id)
                .stream()
                .map(carAccessory -> accessoryMapper.toResponse(carAccessory.getAccessory()))
                .toList();

        // Tráº£ vá» toÃ n bá»™ thÃ´ng tin chi tiáº¿t cho trang giá»›i thiá»‡u xe
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
            UUID parsedStyleId = parseNullableUuid(styleId, "Id kiá»ƒu dÃ¡ng khÃ´ng há»£p lá»‡.");
            UUID parsedSeriesId = parseNullableUuid(seriesId, "Id dÃ²ng xe khÃ´ng há»£p lá»‡.");

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

    // TÃ¬m phiÃªn báº£n xe theo id
    private CarVersion findCarVersion(String carVersionId) {
        // Chuyá»ƒn id phiÃªn báº£n xe sang UUID
        UUID id = parseUuid(carVersionId, "Id phiÃªn báº£n xe khÃ´ng há»£p lá»‡.");

        // Láº¥y phiÃªn báº£n xe tá»« csdl
        return carVersionRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "KhÃ´ng tÃ¬m tháº¥y phiÃªn báº£n xe."));
    }

    // Chuyá»ƒn chuá»—i id sang UUID
    private UUID parseUuid(String value, String message) {
        try {
            // Chuyá»ƒn chuá»—i há»£p lá»‡ sang UUID
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            // Tráº£ lá»—i náº¿u chuá»—i khÃ´ng Ä‘Ãºng Ä‘á»‹nh dáº¡ng UUID
            throw new CustomException(400, message);
        }
    }

    // Chuyá»ƒn chuá»—i id cÃ³ thá»ƒ null sang UUID
    private UUID parseNullableUuid(String value, String message) {
        // Bá» qua Ä‘iá»u kiá»‡n lá»c náº¿u id khÃ´ng Ä‘Æ°á»£c truyá»n vÃ o
        if (!StringUtils.hasText(value)) {
            return null;
        }

        // Chuyá»ƒn id sang UUID khi cÃ³ dá»¯ liá»‡u
        return parseUuid(value, message);
    }

    // Chuáº©n hÃ³a keyword rá»—ng thÃ nh null
    private String normalizeText(String value) {
        // Tráº£ null náº¿u chuá»—i khÃ´ng cÃ³ ná»™i dung
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toLikePattern(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? "%" : "%" + normalized.toLowerCase() + "%";
    }

    // Chuẩn hóa kích thước trang
    private int normalizeSize(int size) {
        // DÃ¹ng kÃ­ch thÆ°á»›c máº·c Ä‘á»‹nh náº¿u request truyá»n giÃ¡ trá»‹ khÃ´ng há»£p lá»‡
        if (size <= 0) {
            return 20;
        }

        // Giá»›i háº¡n kÃ­ch thÆ°á»›c trang Ä‘á»ƒ trÃ¡nh truy váº¥n quÃ¡ lá»›n
        return Math.min(size, 50);
    }
}
