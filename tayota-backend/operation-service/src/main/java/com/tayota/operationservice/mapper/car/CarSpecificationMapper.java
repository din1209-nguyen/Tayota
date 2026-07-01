package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.CarSpecificationResponseDTO;
import com.tayota.operationservice.entity.car.CarSpecification;
import org.springframework.stereotype.Component;

@Component
public class CarSpecificationMapper {

    // Chuyển thông số kỹ thuật sang response
    public CarSpecificationResponseDTO toResponse(CarSpecification specification) {
        return new CarSpecificationResponseDTO(
                specification.getCarVersionId(),
                specification.getOrigin(),
                specification.getFuel(),
                specification.getNumberOfSeats(),
                specification.getLength(),
                specification.getWidth(),
                specification.getHeight(),
                specification.getCapacity(),
                specification.getCylinderCapacity(),
                specification.getCylinder(),
                specification.getGearbox(),
                specification.getMaximumSpeed(),
                specification.getAcceleration(),
                specification.getTorque(),
                specification.getGrossWeightAllowance(),
                specification.getTrademarks()
        );
    }
}
