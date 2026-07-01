package com.tayota.operationservice.dto.request.car;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CarArticleRequestDTO {
    private String carVersionId;

    @NotBlank(message = "Loại bài viết không được để trống")
    @Size(max = 50, message = "Loại bài viết không được vượt quá 50 ký tự")
    private String type;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    @Size(max = 1024, message = "Đường dẫn ảnh không được vượt quá 1024 ký tự")
    private String imageUrl;

    private Boolean published = true;
}
