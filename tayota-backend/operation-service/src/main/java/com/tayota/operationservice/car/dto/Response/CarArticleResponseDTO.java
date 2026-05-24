package com.tayota.operationservice.car.dto.Response;

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
    private String imageUrl;
}
