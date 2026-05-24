package com.tayota.operationservice.car.mapper;

import com.tayota.operationservice.car.dto.Response.CarGalleryResponseDTO;
import com.tayota.operationservice.car.entity.CarGallery;
import org.springframework.stereotype.Component;

@Component
public class CarGalleryMapper {

    // Chuyển hình ảnh sang response
    public CarGalleryResponseDTO toResponse(CarGallery gallery) {
        return new CarGalleryResponseDTO(gallery.getId(), gallery.getImageUrl());
    }
}
