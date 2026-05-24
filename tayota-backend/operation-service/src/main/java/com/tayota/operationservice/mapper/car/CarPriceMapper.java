package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.CarPriceResponseDTO;
import com.tayota.operationservice.entity.car.CarPrice;
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
