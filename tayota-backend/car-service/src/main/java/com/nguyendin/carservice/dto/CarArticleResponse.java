package com.nguyendin.carservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record CarArticleResponse(
        UUID id,
        String type,
        String title,
        String content,
        @JsonProperty("image_url")
        String imageUrl
) {
}
