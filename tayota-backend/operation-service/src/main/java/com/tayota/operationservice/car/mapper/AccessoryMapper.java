package com.tayota.operationservice.car.mapper;

import com.tayota.operationservice.car.dto.Response.AccessoryResponseDTO;
import com.tayota.operationservice.car.entity.Accessory;
import org.springframework.stereotype.Component;

@Component
public class AccessoryMapper {

    // Chuyển phụ kiện sang response
    public AccessoryResponseDTO toResponse(Accessory accessory) {
        return new AccessoryResponseDTO(
                accessory.getId(),
                accessory.getModel(),
                accessory.getBrand(),
                accessory.getPrice(),
                accessory.getDescription(),
                accessory.getUseContent(),
                accessory.getReminderContent(),
                accessory.getType()
        );
    }
}
