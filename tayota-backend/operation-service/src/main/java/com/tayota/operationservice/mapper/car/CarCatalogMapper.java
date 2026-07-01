package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.AccessoryResponseDTO;
import com.tayota.operationservice.dto.response.car.CarArticleResponseDTO;
import com.tayota.operationservice.dto.response.car.CarGalleryResponseDTO;
import com.tayota.operationservice.dto.response.car.CarPriceResponseDTO;
import com.tayota.operationservice.dto.response.car.CarSeriesWithVersionsResponseDTO;
import com.tayota.operationservice.dto.response.car.CarSpecificationResponseDTO;
import com.tayota.operationservice.dto.response.car.CarStyleWithVersionsResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionDetailResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionItemResponseDTO;
import com.tayota.operationservice.entity.car.CarAccessory;
import com.tayota.operationservice.entity.car.CarArticle;
import com.tayota.operationservice.entity.car.CarGallery;
import com.tayota.operationservice.entity.car.CarPrice;
import com.tayota.operationservice.entity.car.CarSeries;
import com.tayota.operationservice.entity.car.CarSpecification;
import com.tayota.operationservice.entity.car.CarStyle;
import com.tayota.operationservice.entity.car.CarVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CarCatalogMapper {
    private final AccessoryMapper accessoryMapper;
    private final CarArticleMapper carArticleMapper;
    private final CarGalleryMapper carGalleryMapper;
    private final CarPriceMapper carPriceMapper;
    private final CarSeriesMapper carSeriesMapper;
    private final CarSpecificationMapper carSpecificationMapper;
    private final CarStyleMapper carStyleMapper;
    private final CarVersionMapper carVersionMapper;

    // Chuyển kiểu dáng sang response kèm danh sách dòng xe và phiên bản.
    public CarStyleWithVersionsResponseDTO toStyleWithVersions(
            CarStyle style,
            List<CarSeriesWithVersionsResponseDTO> series
    ) {
        // Trả về kiểu dáng đã gom các dòng xe còn phiên bản hiển thị.
        return carStyleMapper.toWithVersions(style, series);
    }

    // Chuyển dòng xe sang response kèm danh sách phiên bản.
    public CarSeriesWithVersionsResponseDTO toSeriesWithVersions(
            CarSeries series,
            List<CarVersionItemResponseDTO> versions
    ) {
        // Trả về dòng xe đã gom các phiên bản thuộc cùng dòng.
        return carSeriesMapper.toWithVersions(series, versions);
    }

    // Chuyển phiên bản xe sang response rút gọn cho danh sách.
    public CarVersionItemResponseDTO toVersionItem(
            CarVersion carVersion,
            List<CarPrice> prices,
            List<CarGallery> galleries,
            CarSpecification specification
    ) {
        // Lấy giá thấp nhất để hiển thị ở danh sách xe.
        BigDecimal minPrice = prices.stream()
                .map(CarPrice::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);

        // Lấy ảnh đại diện theo thứ tự ưu tiên: ảnh version, ảnh giá, ảnh gallery.
        String imageUrl = resolveImageUrl(carVersion, prices, galleries);

        // Chuyển thông số kỹ thuật sang DTO nếu xe đã có thông số.
        CarSpecificationResponseDTO specificationResponse = specification == null
                ? null
                : carSpecificationMapper.toResponse(specification);

        // Trả về response item dùng cho danh sách và nhóm kiểu dáng.
        return carVersionMapper.toItem(carVersion, minPrice, imageUrl, specificationResponse);
    }

    // Chuyển phiên bản xe sang response chi tiết cho trang giới thiệu xe.
    public CarVersionDetailResponseDTO toVersionDetail(
            CarVersion carVersion,
            CarSpecification specification,
            List<CarPrice> prices,
            List<CarGallery> galleries,
            List<CarArticle> articles,
            List<CarAccessory> accessories
    ) {
        // Chuyển thông số kỹ thuật sang DTO nếu tồn tại.
        CarSpecificationResponseDTO specificationResponse = specification == null
                ? null
                : carSpecificationMapper.toResponse(specification);

        // Chuyển danh sách giá sang DTO theo từng tổ hợp màu.
        List<CarPriceResponseDTO> priceResponses = prices.stream()
                .map(carPriceMapper::toResponse)
                .toList();

        // Chuyển danh sách hình ảnh sang DTO.
        List<CarGalleryResponseDTO> galleryResponses = galleries.stream()
                .map(carGalleryMapper::toResponse)
                .toList();

        // Chuyển danh sách bài viết giới thiệu sang DTO.
        List<CarArticleResponseDTO> articleResponses = articles.stream()
                .map(carArticleMapper::toResponse)
                .toList();

        // Chuyển danh sách phụ kiện đang hiển thị sang DTO.
        List<AccessoryResponseDTO> accessoryResponses = accessories.stream()
                .filter(carAccessory -> carAccessory.getAccessory().isVisible())
                .map(carAccessory -> accessoryMapper.toResponse(carAccessory.getAccessory()))
                .toList();

        // Trả về toàn bộ dữ liệu chi tiết phục vụ trang giới thiệu xe.
        return carVersionMapper.toDetail(
                carVersion,
                carSeriesMapper.toResponse(carVersion.getCarSeries()),
                specificationResponse,
                priceResponses,
                galleryResponses,
                articleResponses,
                accessoryResponses
        );
    }

    // Lấy ảnh đại diện tốt nhất cho phiên bản xe.
    private String resolveImageUrl(CarVersion carVersion, List<CarPrice> prices, List<CarGallery> galleries) {
        // Ưu tiên ảnh đại diện riêng của phiên bản xe.
        if (StringUtils.hasText(carVersion.getImageUrl())) {
            return carVersion.getImageUrl();
        }

        // Lấy ảnh ngoại thất đầu tiên trong dữ liệu giá nếu chưa có ảnh riêng.
        return prices.stream()
                .map(CarPrice::getExImageUrl)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseGet(() -> galleries.stream()
                        .map(CarGallery::getImageUrl)
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .orElse(null));
    }
}
