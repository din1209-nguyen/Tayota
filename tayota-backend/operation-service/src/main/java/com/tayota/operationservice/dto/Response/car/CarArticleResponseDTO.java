package com.tayota.operationservice.dto.response.car;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarArticleResponseDTO {
    private UUID id;
    private String type;
    private String title;
    private String content;
    private String imageUrl;
    private UUID carVersionId;
    private boolean published;
    private Instant createdAt;
    private Instant updatedAt;
}
