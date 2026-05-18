package com.tayota.carservice.mapper;

import com.tayota.carservice.dto.Response.CarPriceResponseDTO;
import com.tayota.carservice.entity.CarPrice;
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
