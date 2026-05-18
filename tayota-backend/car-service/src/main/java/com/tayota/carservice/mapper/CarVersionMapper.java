package com.tayota.carservice.mapper;

import com.tayota.carservice.dto.Response.AccessoryResponseDTO;
import com.tayota.carservice.dto.Response.CarArticleResponseDTO;
import com.tayota.carservice.dto.Response.CarGalleryResponseDTO;
import com.tayota.carservice.dto.Response.CarPriceResponseDTO;
import com.tayota.carservice.dto.Response.CarSeriesResponseDTO;
import com.tayota.carservice.dto.Response.CarSpecificationResponseDTO;
import com.tayota.carservice.dto.Response.CarVersionDetailResponseDTO;
import com.tayota.carservice.dto.Response.CarVersionItemResponseDTO;
import com.tayota.carservice.entity.CarSeries;
import com.tayota.carservice.entity.CarStyle;
import com.tayota.carservice.entity.CarVersion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CarVersionMapper {

    // Chuyển phiên bản xe sang response danh sách
    public CarVersionItemResponseDTO toItem(CarVersion carVersion, BigDecimal minPrice, String imageUrl) {
        CarSeries carSeries = carVersion.getCarSeries();
        CarStyle carStyle = carSeries.getCarStyle();

        return new CarVersionItemResponseDTO(
                carVersion.getId(),
                carVersion.getName(),
                carVersion.getModelYear(),
                carVersion.getSalePercent(),
                carSeries.getId(),
                carSeries.getName(),
                carStyle.getId(),
                carStyle.getName(),
                minPrice,
                imageUrl
        );
    }

    // Chuyển phiên bản xe sang response chi tiết
    public CarVersionDetailResponseDTO toDetail(
            CarVersion carVersion,
            CarSeriesResponseDTO carSeries,
            CarSpecificationResponseDTO specification,
            List<CarPriceResponseDTO> prices,
            List<CarGalleryResponseDTO> galleries,
            List<CarArticleResponseDTO> articles,
            List<AccessoryResponseDTO> accessories
    ) {
        return new CarVersionDetailResponseDTO(
                carVersion.getId(),
                carVersion.getName(),
                carVersion.getModelYear(),
                carVersion.getSalePercent(),
                carVersion.getVideoUrl(),
                carSeries,
                specification,
                prices,
                galleries,
                articles,
                accessories
        );
    }
}
