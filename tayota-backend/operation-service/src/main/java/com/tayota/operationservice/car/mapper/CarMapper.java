package com.tayota.operationservice.car.mapper;

import com.tayota.operationservice.car.dto.Response.CarResponseDTO;
import com.tayota.operationservice.car.entity.Car;
import org.springframework.stereotype.Component;

@Component
public class CarMapper {

    // Chuyển xe vật lý sang response
    public CarResponseDTO toResponse(Car car) {
        return new CarResponseDTO(
                car.getVinId(),
                car.getCarVersion().getId(),
                car.getCarVersion().getName(),
                car.getDealership().getId(),
                car.getDealership().getName(),
                car.getEngineNumber(),
                car.getOwnerUserId(),
                car.getStatus(),
                car.getProductedYear()
        );
    }
}
