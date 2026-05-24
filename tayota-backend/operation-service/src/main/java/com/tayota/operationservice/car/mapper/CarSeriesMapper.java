package com.tayota.operationservice.car.mapper;

import com.tayota.operationservice.car.dto.Response.CarSeriesResponseDTO;
import com.tayota.operationservice.car.dto.Response.CarSeriesWithVersionsResponseDTO;
import com.tayota.operationservice.car.dto.Response.CarVersionItemResponseDTO;
import com.tayota.operationservice.car.entity.CarSeries;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CarSeriesMapper {

    // Chuyển dòng xe sang response
    public CarSeriesResponseDTO toResponse(CarSeries carSeries) {
        return new CarSeriesResponseDTO(
                carSeries.getId(),
                carSeries.getCarStyle().getId(),
                carSeries.getCarStyle().getName(),
                carSeries.getName(),
                carSeries.getDescription()
        );
    }

    // Chuyển dòng xe sang response kèm danh sách phiên bản
    public CarSeriesWithVersionsResponseDTO toWithVersions(
            CarSeries carSeries,
            List<CarVersionItemResponseDTO> versions
    ) {
        return new CarSeriesWithVersionsResponseDTO(
                carSeries.getId(),
                carSeries.getName(),
                carSeries.getDescription(),
                versions
        );
    }
}
