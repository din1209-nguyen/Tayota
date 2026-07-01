package com.tayota.operationservice.service.media;

import com.tayota.operationservice.config.CloudinaryProperties;
import com.tayota.operationservice.dto.response.media.MediaUploadResponse;
import com.tayota.operationservice.enums.media.MediaUploadContext;
import com.tayota.operationservice.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryMediaServiceTest {
    @Mock
    private CloudinaryUploader uploader;

    private CloudinaryProperties properties;
    private CloudinaryMediaService service;

    @BeforeEach
    void setUp() {
        properties = new CloudinaryProperties();
        properties.setCloudName("demo");
        properties.setApiKey("key");
        properties.setApiSecret("secret");
        properties.setFolderPrefix("tayota");
        service = new CloudinaryMediaService(properties, uploader);
        authenticateAs("ROLE_MANAGER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadImageReturnsCloudinaryUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "car.png", "image/png", new byte[]{1, 2, 3});
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/car.png",
                "public_id", "tayota/car-gallery/car",
                "resource_type", "image",
                "format", "png",
                "bytes", 3,
                "width", 1200,
                "height", 800
        ));

        MediaUploadResponse response = service.upload(file, MediaUploadContext.CAR_GALLERY);

        assertEquals("https://res.cloudinary.com/demo/image/upload/car.png", response.getSecureUrl());
        assertEquals("tayota/car-gallery/car", response.getPublicId());
        assertEquals("image", response.getResourceType());
    }

    @Test
    void userAvatarAllowsRegularUser() throws Exception {
        authenticateAs("ROLE_USER");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1});
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/avatar.jpg",
                "resource_type", "image"
        ));

        MediaUploadResponse response = service.upload(file, MediaUploadContext.USER_AVATAR);

        assertEquals("https://res.cloudinary.com/demo/image/upload/avatar.jpg", response.getSecureUrl());
    }

    @Test
    void catalogMediaRejectsRegularUser() throws Exception {
        authenticateAs("ROLE_USER");
        MockMultipartFile file = new MockMultipartFile("file", "car.png", "image/png", new byte[]{1});

        CustomException exception = assertThrows(CustomException.class, () -> service.upload(file, MediaUploadContext.CAR_GALLERY));

        assertEquals(403, exception.getCode());
        verify(uploader, never()).upload(any(byte[].class), anyMap());
    }

    @Test
    void imageContextRejectsNonImageFile() {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", new byte[]{1});

        CustomException exception = assertThrows(CustomException.class, () -> service.upload(file, MediaUploadContext.ARTICLE_IMAGE));

        assertEquals(400, exception.getCode());
    }

    @Test
    void missingCloudinaryConfigReturnsUnavailable() {
        properties.setApiSecret("");
        MockMultipartFile file = new MockMultipartFile("file", "car.png", "image/png", new byte[]{1});

        CustomException exception = assertThrows(CustomException.class, () -> service.upload(file, MediaUploadContext.CAR_GALLERY));

        assertEquals(503, exception.getCode());
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user-id",
                null,
                List.of(new SimpleGrantedAuthority(role))
        ));
    }
}
