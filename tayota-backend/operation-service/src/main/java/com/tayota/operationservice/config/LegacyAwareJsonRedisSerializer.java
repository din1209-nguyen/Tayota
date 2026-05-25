package com.tayota.operationservice.config;

import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class LegacyAwareJsonRedisSerializer implements RedisSerializer<Object> {
    private final GenericJacksonJsonRedisSerializer typedSerializer;
    private final ObjectMapper legacyObjectMapper;

    public LegacyAwareJsonRedisSerializer(ObjectMapper objectMapper) {
        this.typedSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);
        this.legacyObjectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        return typedSerializer.serialize(value);
    }

    @Override
    public Object deserialize(byte[] source) throws SerializationException {
        try {
            return typedSerializer.deserialize(source);
        }
        catch (SerializationException exception) {
            try {
                return legacyObjectMapper.readValue(source, Object.class);
            }
            catch (JacksonException legacyException) {
                exception.addSuppressed(legacyException);
                throw exception;
            }
        }
    }
}
