package com.tayota.carservice.service;

import com.tayota.carservice.dto.Request.CreateCarVersionRequestDTO;
import com.tayota.carservice.dto.Response.CarVersionDetailResponseDTO;
import com.tayota.carservice.dto.Response.CarVersionItemResponseDTO;
import com.tayota.carservice.dto.Response.CarVersionListResponseDTO;
import com.tayota.carservice.dto.Response.CreateCarVersionResponseDTO;
import com.tayota.carservice.dto.Response.PaginationResponseDTO;
import com.tayota.carservice.entity.CarSeries;
import com.tayota.carservice.entity.CarVersion;
import com.tayota.carservice.mapper.CarVersionMapper;
import com.tayota.carservice.object.CarVersionViewEvent;
import com.tayota.carservice.object.UserBehaviorLogEvent;
import com.tayota.carservice.repository.CarArticleRepository;
import com.tayota.carservice.repository.CarGalleryRepository;
import com.tayota.carservice.repository.CarPriceRepository;
import com.tayota.carservice.repository.CarSeriesRepository;
import com.tayota.carservice.repository.CarSpecificationRepository;
import com.tayota.carservice.repository.CarVersionRepository;
import com.tayota.carservice.repository.projection.CarVersionListProjection;
import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tayota.carservice.mapper.CarVersionMapper.toDetailResponse;
import static com.tayota.carservice.mapper.CarVersionMapper.toSpecification;

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

    // Lấy danh sách phiên bản xe và cache theo trang
    @Transactional(readOnly = true)
    public CarVersionListResponseDTO getCarVersions(int page, int size) {
        String cacheKey = String.format(CAR_VERSION_CACHE_KEY_PATTERN, page, size);
        CarVersionListResponseDTO cachedResponse = getFromCache(cacheKey);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        Page<CarVersionListProjection> carVersionPage = carVersionRepository.findCarVersionList(PageRequest.of(page, size));
        List<CarVersionItemResponseDTO> data = carVersionPage.getContent()
                .stream()
                .map(CarVersionMapper::toResponse)
                .toList();

        CarVersionListResponseDTO response = new CarVersionListResponseDTO(
                data,
                new PaginationResponseDTO(
                        carVersionPage.getNumber(),
                        carVersionPage.getSize(),
                        carVersionPage.getTotalElements(),
                        carVersionPage.getTotalPages()
                )
        );

        saveToCache(cacheKey, response);
        return response;
    }

    // Tạo phiên bản xe mới
    @Transactional
    public CreateCarVersionResponseDTO createCarVersion(CreateCarVersionRequestDTO request) {
        UUID carSeriesId = parseUuid(request.getCarSeriesId());
        CarSeries carSeries = carSeriesRepository.findById(carSeriesId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy dòng xe"));

        String version = request.getVersion().trim();
        String imageUrl = request.getImageUrl().trim();
        String videoUrl = request.getVideoUrl().trim();

        if (carVersionRepository.existsByCarSeriesIdAndVersion(carSeriesId, version)) {
            throw new CustomException(409, "Phiên bản xe đã tồn tại");
        }

        CarVersion carVersion = CarVersion.builder()
                .carSeries(carSeries)
                .version(version)
                .salePercent(request.getSalePercent())
                .imageUrl(imageUrl)
                .videoUrl(videoUrl)
                .build();

        CarVersion savedCarVersion = carVersionRepository.save(carVersion);
        carSpecificationRepository.save(toSpecification(savedCarVersion, request.getSpecification()));
        invalidateCarVersionListCache();

        return new CreateCarVersionResponseDTO(savedCarVersion.getId(), "Thêm phiên bản xe thành công.");
    }

    // Lấy thông tin chi tiết phiên bản xe
    @Transactional(readOnly = true)
    public CarVersionDetailResponseDTO getCarVersionDetail(UUID id, UUID userId) {
        CarVersion carVersion = carVersionRepository.findWithCarSeriesById(id)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiên bản xe"));

        CarVersionDetailResponseDTO response = toDetailResponse(
                carVersion,
                carSpecificationRepository.findById(id).orElse(null),
                carPriceRepository.findByIdCarVersionId(id),
                carGalleryRepository.findByCarVersionId(id),
                carArticleRepository.findByCarVersionId(id)
        );

        publishViewEvents(id, userId);
        return response;
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

    private CarVersionListResponseDTO getFromCache(String cacheKey) {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

            if (cachedValue == null) {
                return null;
            }

            if (cachedValue instanceof CarVersionListResponseDTO response) {
                return response;
            }

            return objectMapper.convertValue(cachedValue, CarVersionListResponseDTO.class);
        } catch (RuntimeException exception) {
            log.warn("Cannot read car versions cache key {}: {}", cacheKey, exception.getMessage());
            return null;
        }
    }

    private void saveToCache(String cacheKey, CarVersionListResponseDTO response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, response, CAR_VERSION_CACHE_TTL);
        } catch (RuntimeException exception) {
            log.warn("Cannot write car versions cache key {}: {}", cacheKey, exception.getMessage());
        }
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
            throw new CustomException(400, "Id không hợp lệ");
        }
    }
}
