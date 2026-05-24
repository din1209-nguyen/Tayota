package com.tayota.operationservice.car.mapper;

import com.tayota.operationservice.car.dto.Response.CarSeriesWithVersionsResponseDTO;
import com.tayota.operationservice.car.dto.Response.CarStyleResponseDTO;
import com.tayota.operationservice.car.dto.Response.CarStyleWithVersionsResponseDTO;
import com.tayota.operationservice.car.entity.CarStyle;
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
