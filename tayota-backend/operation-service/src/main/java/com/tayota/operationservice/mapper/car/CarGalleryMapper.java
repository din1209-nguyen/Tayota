package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.CarGalleryResponseDTO;
import com.tayota.operationservice.entity.car.CarGallery;
import org.springframework.stereotype.Component;

@Component
public class CarGalleryMapper {

    // Chuyển hình ảnh sang response
    public CarGalleryResponseDTO toResponse(CarGallery gallery) {
        return new CarGalleryResponseDTO(gallery.getId(), gallery.getImageUrl());
    }
}
