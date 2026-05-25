package com.tayota.operationservice.config;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyAwareJsonRedisSerializerTest {
    private final LegacyAwareJsonRedisSerializer serializer =
            new LegacyAwareJsonRedisSerializer(new ObjectMapper());

    @Test
    void deserializeFallsBackToPlainJsonObject() {
        byte[] source = """
                {"refreshHash":"h1","clientIp":"192.168.1.1","userAgent":"Windows","loginAt":"2026-05-04T10:00:00Z"}
                """.getBytes(StandardCharsets.UTF_8);

        Object value = serializer.deserialize(source);

        assertThat(value)
                .isInstanceOf(Map.class)
                .extracting("refreshHash", "clientIp", "userAgent", "loginAt")
                .containsExactly("h1", "192.168.1.1", "Windows", "2026-05-04T10:00:00Z");
    }
}
