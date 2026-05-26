package com.tayota.operationservice.service.media;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DefaultCloudinaryUploader implements CloudinaryUploader {
    private final Cloudinary cloudinary;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> upload(byte[] fileBytes, Map<String, Object> options) throws IOException {
        return cloudinary.uploader().upload(fileBytes, options);
    }
}
