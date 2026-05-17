package com.nguyendin.carservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record CarGalleryResponse(
        UUID id,
        @JsonProperty("image_url")
        String imageUrl
) {
}
