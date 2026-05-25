package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.AccessoryResponseDTO;
import com.tayota.operationservice.entity.car.Accessory;
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
                accessory.getType(),
                accessory.isVisible()
        );
    }
}
