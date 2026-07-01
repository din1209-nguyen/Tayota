package com.tayota.operationservice.service.media;

import com.tayota.operationservice.config.CloudinaryProperties;
import com.tayota.operationservice.dto.response.media.MediaUploadResponse;
import com.tayota.operationservice.enums.media.MediaUploadContext;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryMediaService {
    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    private static final long MAX_VIDEO_BYTES = 100L * 1024L * 1024L;

    private final CloudinaryProperties properties;
    private final CloudinaryUploader uploader;

    public MediaUploadResponse upload(MultipartFile file, MediaUploadContext context) {
        validateRole(context);
        validateFile(file, context);
        if (!properties.isConfigured()) {
            throw new CustomException(503, "Cloudinary chưa được cấu hình.");
        }

        try {
            Map<String, Object> result = uploader.upload(file.getBytes(), Map.of(
                    "resource_type", "auto",
                    "folder", buildFolder(context),
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false
            ));
            return toResponse(result);
        } catch (IOException | RuntimeException exception) {
            throw new CustomException(503, "Không thể tải media lên Cloudinary.");
        }
    }

    private void validateRole(MediaUploadContext context) {
        String role = SecurityContextUtil.getCurrentUserRole();
        if (context == MediaUploadContext.USER_AVATAR) {
            return;
        }
        if (!"ROLE_ADMIN".equals(role) && !"ROLE_MANAGER".equals(role)) {
            throw new CustomException(403, "Bạn không có quyền tải media quản trị.");
        }
    }

    private void validateFile(MultipartFile file, MediaUploadContext context) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(400, "File tải lên không được để trống.");
        }

        String contentType = file.getContentType();
        boolean video = context == MediaUploadContext.CAR_VIDEO;
        if (!StringUtils.hasText(contentType)
                || (video && !contentType.toLowerCase(Locale.ROOT).startsWith("video/"))
                || (!video && !contentType.toLowerCase(Locale.ROOT).startsWith("image/"))) {
            throw new CustomException(400, video ? "File video không hợp lệ." : "File ảnh không hợp lệ.");
        }

        long maxBytes = video ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES;
        if (file.getSize() > maxBytes) {
            throw new CustomException(400, video ? "Video không được vượt quá 100MB." : "Ảnh không được vượt quá 10MB.");
        }
    }

    private String buildFolder(MediaUploadContext context) {
        String prefix = StringUtils.hasText(properties.getFolderPrefix()) ? properties.getFolderPrefix().trim() : "tayota";
        return prefix + "/" + context.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private MediaUploadResponse toResponse(Map<String, Object> result) {
        return new MediaUploadResponse(
                stringValue(result.get("secure_url")),
                stringValue(result.get("public_id")),
                stringValue(result.get("resource_type")),
                stringValue(result.get("format")),
                longValue(result.get("bytes")),
                integerValue(result.get("width")),
                integerValue(result.get("height")),
                doubleValue(result.get("duration"))
        );
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
