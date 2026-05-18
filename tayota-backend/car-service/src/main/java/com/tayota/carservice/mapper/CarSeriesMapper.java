package com.tayota.carservice.mapper;

import com.tayota.carservice.dto.Response.CarSeriesResponseDTO;
import com.tayota.carservice.dto.Response.CarSeriesWithVersionsResponseDTO;
import com.tayota.carservice.dto.Response.CarVersionItemResponseDTO;
import com.tayota.carservice.entity.CarSeries;
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
