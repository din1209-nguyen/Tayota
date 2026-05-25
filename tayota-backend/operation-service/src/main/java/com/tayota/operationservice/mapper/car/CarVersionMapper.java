package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.AccessoryResponseDTO;
import com.tayota.operationservice.dto.response.car.CarArticleResponseDTO;
import com.tayota.operationservice.dto.response.car.CarGalleryResponseDTO;
import com.tayota.operationservice.dto.response.car.CarPriceResponseDTO;
import com.tayota.operationservice.dto.response.car.CarSeriesResponseDTO;
import com.tayota.operationservice.dto.response.car.CarSpecificationResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionDetailResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionItemResponseDTO;
import com.tayota.operationservice.entity.car.CarSeries;
import com.tayota.operationservice.entity.car.CarStyle;
import com.tayota.operationservice.entity.car.CarVersion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CarVersionMapper {

    // Chuyển phiên bản xe sang response danh sách
    public CarVersionItemResponseDTO toItem(CarVersion carVersion, BigDecimal minPrice, String imageUrl) {
        return toItem(carVersion, minPrice, imageUrl, null);
    }

    public CarVersionItemResponseDTO toItem(
            CarVersion carVersion,
            BigDecimal minPrice,
            String imageUrl,
            CarSpecificationResponseDTO specification
    ) {
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
                imageUrl,
                specification,
                carVersion.getVideoUrl(),
                carVersion.isVisible()
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
                accessories,
                carVersion.isVisible()
        );
    }
}
