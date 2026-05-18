package com.tayota.carservice.mapper;

import com.tayota.carservice.dto.Request.CreateCarSpecificationRequestDTO;
import com.tayota.carservice.dto.Response.CarArticleResponseDTO;
import com.tayota.carservice.dto.Response.CarGalleryResponseDTO;
import com.tayota.carservice.dto.Response.CarPriceResponseDTO;
import com.tayota.carservice.dto.Response.CarSeriesResponseDTO;
import com.tayota.carservice.dto.Response.CarSpecificationResponseDTO;
import com.tayota.carservice.dto.Response.CarStyleResponseDTO;
import com.tayota.carservice.dto.Response.CarVersionDetailResponseDTO;
import com.tayota.carservice.dto.Response.CarVersionItemResponseDTO;
import com.tayota.carservice.entity.CarArticle;
import com.tayota.carservice.entity.CarGallery;
import com.tayota.carservice.entity.CarPrice;
import com.tayota.carservice.entity.CarSeries;
import com.tayota.carservice.entity.CarSpecification;
import com.tayota.carservice.entity.CarStyle;
import com.tayota.carservice.entity.CarVersion;
import com.tayota.carservice.repository.projection.CarVersionListProjection;

import java.util.List;

public class CarVersionMapper {
    public static CarVersionItemResponseDTO toResponse(CarVersionListProjection projection) {
        return new CarVersionItemResponseDTO(
                projection.getId(),
                projection.getVersion(),
                projection.getSeries(),
                projection.getStyle(),
                projection.getMinPrice(),
                projection.getSalePercent(),
                projection.getImageUrl()
        );
    }

    public static CarVersionDetailResponseDTO toDetailResponse(
            CarVersion carVersion,
            CarSpecification specification,
            List<CarPrice> prices,
            List<CarGallery> gallery,
            List<CarArticle> articles
    ) {
        return new CarVersionDetailResponseDTO(
                carVersion.getId(),
                carVersion.getVersion(),
                toSeriesResponse(carVersion.getCarSeries()),
                toSpecificationResponse(specification),
                prices.stream().map(CarVersionMapper::toPriceResponse).toList(),
                gallery.stream().map(CarVersionMapper::toGalleryResponse).toList(),
                articles.stream().map(CarVersionMapper::toArticleResponse).toList(),
                carVersion.getSalePercent()
        );
    }

    public static CarSpecification toSpecification(CarVersion carVersion, CreateCarSpecificationRequestDTO request) {
        return CarSpecification.builder()
                .carVersion(carVersion)
                .origin(request.getOrigin().trim())
                .fuel(request.getFuel().trim())
                .numberOfSeats(request.getNumberOfSeats())
                .length(request.getLength())
                .width(request.getWidth())
                .height(request.getHeight())
                .capacity(request.getCapacity())
                .cylinderCapacity(trimToNull(request.getCylinderCapacity()))
                .cylinder(request.getCylinder())
                .gearbox(trimToNull(request.getGearbox()))
                .maximumSpeed(request.getMaximumSpeed())
                .acceleration(trimToNull(request.getAcceleration()))
                .torque(trimToNull(request.getTorque()))
                .grossWeightAllowance(request.getGrossWeightAllowance())
                .trademarks(trimToNull(request.getTrademarks()))
                .build();
    }

    private static CarSeriesResponseDTO toSeriesResponse(CarSeries series) {
        return new CarSeriesResponseDTO(
                series.getId(),
                series.getName(),
                series.getDescription(),
                toStyleResponse(series.getCarStyle())
        );
    }

    private static CarStyleResponseDTO toStyleResponse(CarStyle style) {
        return new CarStyleResponseDTO(
                style.getId(),
                style.getName(),
                style.getDescription()
        );
    }

    private static CarSpecificationResponseDTO toSpecificationResponse(CarSpecification specification) {
        if (specification == null) {
            return null;
        }

        return new CarSpecificationResponseDTO(
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

    private static CarPriceResponseDTO toPriceResponse(CarPrice price) {
        return new CarPriceResponseDTO(
                price.getExteriorColor().getColorName(),
                price.getInteriorColor().getColorName(),
                price.getPrice()
        );
    }

    private static CarGalleryResponseDTO toGalleryResponse(CarGallery gallery) {
        return new CarGalleryResponseDTO(gallery.getId(), gallery.getImageUrl());
    }

    private static CarArticleResponseDTO toArticleResponse(CarArticle article) {
        return new CarArticleResponseDTO(
                article.getId(),
                article.getType(),
                article.getTitle(),
                article.getContent(),
                article.getImageUrl()
        );
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
