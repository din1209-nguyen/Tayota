package com.tayota.carservice.mapper;

import com.tayota.carservice.dto.Response.AccessoryResponseDTO;
import com.tayota.carservice.entity.Accessory;
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
