package com.tayota.operationservice.service.media;

import java.io.IOException;
import java.util.Map;

public interface CloudinaryUploader {
    Map<String, Object> upload(byte[] fileBytes, Map<String, Object> options) throws IOException;
}
