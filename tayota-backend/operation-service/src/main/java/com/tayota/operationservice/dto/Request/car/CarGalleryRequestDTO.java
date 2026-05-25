package com.tayota.operationservice.dto.request.car;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CarGalleryRequestDTO {
    @NotBlank(message = "Đường dẫn ảnh không được để trống")
    @Size(max = 255, message = "Đường dẫn ảnh không được vượt quá 255 ký tự")
    private String imageUrl;
}
