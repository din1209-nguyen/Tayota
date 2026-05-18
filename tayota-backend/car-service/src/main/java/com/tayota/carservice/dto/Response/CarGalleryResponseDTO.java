package com.tayota.carservice.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarGalleryResponseDTO {
    private UUID id;

    @JsonProperty("image_url")
    private String imageUrl;
}
