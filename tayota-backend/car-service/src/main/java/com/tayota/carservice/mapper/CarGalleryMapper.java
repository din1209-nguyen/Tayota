package com.tayota.carservice.mapper;

import com.tayota.carservice.dto.Response.CarGalleryResponseDTO;
import com.tayota.carservice.entity.CarGallery;
import org.springframework.stereotype.Component;

@Component
public class CarGalleryMapper {

    // Chuyển hình ảnh sang response
    public CarGalleryResponseDTO toResponse(CarGallery gallery) {
        return new CarGalleryResponseDTO(gallery.getId(), gallery.getImageUrl());
    }
}
