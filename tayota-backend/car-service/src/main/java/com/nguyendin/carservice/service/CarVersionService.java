package com.nguyendin.carservice.service;

import com.nguyendin.carservice.dto.*;
import com.nguyendin.carservice.entity.*;
import com.nguyendin.carservice.repository.CarArticleRepository;
import com.nguyendin.carservice.repository.CarGalleryRepository;
import com.nguyendin.carservice.repository.CarPriceRepository;
import com.nguyendin.carservice.repository.CarSeriesRepository;
import com.nguyendin.carservice.repository.CarSpecificationRepository;
import com.nguyendin.carservice.repository.CarVersionRepository;
import com.nguyendin.carservice.repository.projection.CarVersionListProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarVersionService {
    private static final Duration CAR_VERSION_CACHE_TTL = Duration.ofMinutes(10);
    private static final String CAR_VERSION_CACHE_KEY_PATTERN = "cars:versions:page:%d:size:%d";
    private static final String CAR_VERSION_CACHE_KEY_SCAN_PATTERN = "cars:versions:page:*:size:*";
    private static final String VIEW_CAR_VERSION_ACTION = "view_car_version";

    private final CarVersionRepository carVersionRepository;
    private final CarSeriesRepository carSeriesRepository;
    private final CarSpecificationRepository carSpecificationRepository;
    private final CarPriceRepository carPriceRepository;
    private final CarGalleryRepository carGalleryRepository;
    private final CarArticleRepository carArticleRepository;
    private final CarViewEventProducer carViewEventProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CarVersionListResponse getCarVersions(int page, int size) {
        String cacheKey = String.format(CAR_VERSION_CACHE_KEY_PATTERN, page, size);
        CarVersionListResponse cachedResponse = getFromCache(cacheKey);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        Page<CarVersionListProjection> carVersionPage = carVersionRepository.findCarVersionList(PageRequest.of(page, size));
        List<CarVersionItemResponse> data = carVersionPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        CarVersionListResponse response = new CarVersionListResponse(
                data,
                new PaginationResponse(
                        carVersionPage.getNumber(),
                        carVersionPage.getSize(),
                        carVersionPage.getTotalElements(),
                        carVersionPage.getTotalPages()
                )
        );

        saveToCache(cacheKey, response);
        return response;
    }

    @Transactional
    public CreateCarVersionResponse createCarVersion(CreateCarVersionRequest request) {
        UUID carSeriesId = parseUuid(request.carSeriesId());
        if (carSeriesId == null) {
            throw new IllegalArgumentException("Sai dữ liệu");
        }

        CarSeries carSeries = carSeriesRepository.findById(carSeriesId)
                .orElseThrow(() -> new IllegalArgumentException("Sai dữ liệu"));
        String version = request.version().trim();
        String imageUrl = request.imageUrl().trim();
        String videoUrl = request.videoUrl().trim();

        if (carVersionRepository.existsByCarSeriesIdAndVersion(carSeriesId, version)) {
            throw new IllegalArgumentException("Sai dữ liệu");
        }

        CarVersion carVersion = CarVersion.builder()
                .carSeries(carSeries)
                .version(version)
                .salePercent(request.salePercent())
                .imageUrl(imageUrl)
                .videoUrl(videoUrl)
                .build();

        CarVersion savedCarVersion = carVersionRepository.save(carVersion);
        carSpecificationRepository.save(toSpecification(savedCarVersion, request.specification()));
        invalidateCarVersionListCache();

        return new CreateCarVersionResponse(savedCarVersion.getId(), "Thêm phiên bản xe thành công.");
    }

    @Transactional(readOnly = true)
    public Optional<CarVersionDetailResponse> getCarVersionDetail(UUID id, UUID userId) {
        return carVersionRepository.findWithCarSeriesById(id)
                .map(carVersion -> {
                    CarVersionDetailResponse response = toDetailResponse(
                            carVersion,
                            carSpecificationRepository.findById(id).orElse(null),
                            carPriceRepository.findByIdCarVersionId(id),
                            carGalleryRepository.findByCarVersionId(id),
                            carArticleRepository.findByCarVersionId(id)
                    );

                    publishViewEvents(id, userId);
                    return response;
                });
    }

    private CarVersionItemResponse toResponse(CarVersionListProjection projection) {
        return new CarVersionItemResponse(
                projection.getId(),
                projection.getVersion(),
                projection.getSeries(),
                projection.getStyle(),
                projection.getMinPrice(),
                projection.getSalePercent(),
                projection.getImageUrl()
        );
    }

    private CarVersionDetailResponse toDetailResponse(
            CarVersion carVersion,
            CarSpecification specification,
            List<CarPrice> prices,
            List<CarGallery> gallery,
            List<CarArticle> articles
    ) {
        return new CarVersionDetailResponse(
                carVersion.getId(),
                carVersion.getVersion(),
                toSeriesResponse(carVersion.getCarSeries()),
                toSpecificationResponse(specification),
                prices.stream().map(this::toPriceResponse).toList(),
                gallery.stream().map(this::toGalleryResponse).toList(),
                articles.stream().map(this::toArticleResponse).toList(),
                carVersion.getSalePercent()
        );
    }

    private CarSeriesResponse toSeriesResponse(CarSeries series) {
        return new CarSeriesResponse(
                series.getId(),
                series.getName(),
                series.getDescription(),
                toStyleResponse(series.getCarStyle())
        );
    }

    private CarStyleResponse toStyleResponse(CarStyle style) {
        return new CarStyleResponse(
                style.getId(),
                style.getName(),
                style.getDescription()
        );
    }

    private CarSpecificationResponse toSpecificationResponse(CarSpecification specification) {
        if (specification == null) {
            return null;
        }

        return new CarSpecificationResponse(
                specification.getCarVersionId(),
                specification.getOrigin(),
                specification.getFuel(),
                specification.getNumberOfSeats(),
                specification.getLength(),
                specification.getWidth(),
                specification.getHeight(),
                specification.getCapacity(),
                specification.getCylinderCapacity(),
                specification.getCylinder(),
                specification.getGearbox(),
                specification.getMaximumSpeed(),
                specification.getAcceleration(),
                specification.getTorque(),
                specification.getGrossWeightAllowance(),
                specification.getTrademarks()
        );
    }

    private CarPriceResponse toPriceResponse(CarPrice price) {
        return new CarPriceResponse(
                price.getExteriorColor().getColorName(),
                price.getInteriorColor().getColorName(),
                price.getPrice()
        );
    }

    private CarGalleryResponse toGalleryResponse(CarGallery gallery) {
        return new CarGalleryResponse(gallery.getId(), gallery.getImageUrl());
    }

    private CarArticleResponse toArticleResponse(CarArticle article) {
        return new CarArticleResponse(
                article.getId(),
                article.getType(),
                article.getTitle(),
                article.getContent(),
                article.getImageUrl()
        );
    }

    private void publishViewEvents(UUID carVersionId, UUID userId) {
        Instant now = Instant.now();

        if (userId != null) {
            carViewEventProducer.sendViewHistory(new CarVersionViewEvent(userId, carVersionId, now));
        }

        carViewEventProducer.sendBehaviorLog(new UserBehaviorLogEvent(
                userId,
                VIEW_CAR_VERSION_ACTION,
                "View car version " + carVersionId,
                now
        ));
    }

    private CarVersionListResponse getFromCache(String cacheKey) {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

            if (cachedValue == null) {
                return null;
            }

            if (cachedValue instanceof CarVersionListResponse response) {
                return response;
            }

            return objectMapper.convertValue(cachedValue, CarVersionListResponse.class);
        } catch (RuntimeException exception) {
            log.warn("Cannot read car versions cache key {}: {}", cacheKey, exception.getMessage());
            return null;
        }
    }

    private void saveToCache(String cacheKey, CarVersionListResponse response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, response, CAR_VERSION_CACHE_TTL);
        } catch (RuntimeException exception) {
            log.warn("Cannot write car versions cache key {}: {}", cacheKey, exception.getMessage());
        }
    }

    private CarSpecification toSpecification(CarVersion carVersion, CreateCarSpecificationRequest request) {
        return CarSpecification.builder()
                .carVersion(carVersion)
                .origin(request.origin().trim())
                .fuel(request.fuel().trim())
                .numberOfSeats(request.numberOfSeats())
                .length(request.length())
                .width(request.width())
                .height(request.height())
                .capacity(request.capacity())
                .cylinderCapacity(trimToNull(request.cylinderCapacity()))
                .cylinder(request.cylinder())
                .gearbox(trimToNull(request.gearbox()))
                .maximumSpeed(request.maximumSpeed())
                .acceleration(trimToNull(request.acceleration()))
                .torque(trimToNull(request.torque()))
                .grossWeightAllowance(request.grossWeightAllowance())
                .trademarks(trimToNull(request.trademarks()))
                .build();
    }

    private void invalidateCarVersionListCache() {
        try {
            Set<String> keys = redisTemplate.keys(CAR_VERSION_CACHE_KEY_SCAN_PATTERN);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException exception) {
            log.warn("Cannot invalidate car versions cache: {}", exception.getMessage());
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
