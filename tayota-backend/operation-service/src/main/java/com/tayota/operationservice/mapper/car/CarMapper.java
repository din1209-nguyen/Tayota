package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.CarResponseDTO;
import com.tayota.operationservice.entity.car.Car;
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
