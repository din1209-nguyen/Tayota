package com.tayota.operationservice.car.mapper;

import com.tayota.operationservice.car.dto.Response.CarPriceResponseDTO;
import com.tayota.operationservice.car.entity.CarPrice;
import org.springframework.stereotype.Component;

@Component
public class CarPriceMapper {

    // Chuyển giá xe sang response
    public CarPriceResponseDTO toResponse(CarPrice carPrice) {
        return new CarPriceResponseDTO(
                carPrice.getExteriorColor().getId(),
                carPrice.getExteriorColor().getColorName(),
                carPrice.getInteriorColor().getId(),
                carPrice.getInteriorColor().getColorName(),
                carPrice.getPrice(),
                carPrice.getExImageUrl(),
                carPrice.getInImageUrl()
        );
    }
}
