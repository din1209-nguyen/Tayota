package com.tayota.operationservice.dto.response.media;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MediaUploadResponse {
    private String secureUrl;
    private String publicId;
    private String resourceType;
    private String format;
    private Long bytes;
    private Integer width;
    private Integer height;
    private Double duration;
}
