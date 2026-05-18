package com.tayota.carservice.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarArticleResponseDTO {
    private UUID id;
    private String type;
    private String title;
    private String content;

    @JsonProperty("image_url")
    private String imageUrl;
}
