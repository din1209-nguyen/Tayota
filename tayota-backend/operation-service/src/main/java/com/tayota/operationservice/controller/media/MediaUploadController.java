package com.tayota.operationservice.controller.media;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.response.media.MediaUploadResponse;
import com.tayota.operationservice.enums.media.MediaUploadContext;
import com.tayota.operationservice.service.media.CloudinaryMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media/uploads")
public class MediaUploadController {
    private final CloudinaryMediaService cloudinaryMediaService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ApiResponse<MediaUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("context") MediaUploadContext context
    ) {
        return ApiResponse.success(201, "Tải media lên Cloudinary thành công.", cloudinaryMediaService.upload(file, context));
    }
}
