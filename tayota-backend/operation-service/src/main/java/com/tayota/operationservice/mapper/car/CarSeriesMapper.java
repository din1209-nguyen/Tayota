package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.CarSeriesResponseDTO;
import com.tayota.operationservice.dto.response.car.CarSeriesWithVersionsResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionItemResponseDTO;
import com.tayota.operationservice.entity.car.CarSeries;
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
