package com.tayota.carservice.mapper;

import com.tayota.carservice.dto.Response.CarSeriesWithVersionsResponseDTO;
import com.tayota.carservice.dto.Response.CarStyleResponseDTO;
import com.tayota.carservice.dto.Response.CarStyleWithVersionsResponseDTO;
import com.tayota.carservice.entity.CarStyle;
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
