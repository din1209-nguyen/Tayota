package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.CarSeriesWithVersionsResponseDTO;
import com.tayota.operationservice.dto.response.car.CarStyleResponseDTO;
import com.tayota.operationservice.dto.response.car.CarStyleWithVersionsResponseDTO;
import com.tayota.operationservice.entity.car.CarStyle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CarStyleMapper {

    // Chuyển kiểu dáng sang response
    public CarStyleResponseDTO toResponse(CarStyle carStyle) {
        return new CarStyleResponseDTO(
                carStyle.getId(),
                carStyle.getName(),
                carStyle.getDescription()
        );
    }

    // Chuyển kiểu dáng sang response kèm danh sách dòng xe
    public CarStyleWithVersionsResponseDTO toWithVersions(
            CarStyle carStyle,
            List<CarSeriesWithVersionsResponseDTO> series
    ) {
        return new CarStyleWithVersionsResponseDTO(
                carStyle.getId(),
                carStyle.getName(),
                carStyle.getDescription(),
                series
        );
    }
}
