package com.tayota.operationservice.config;

import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

public class LegacyAwareJsonRedisSerializer implements RedisSerializer<Object> {
    private final GenericJacksonJsonRedisSerializer typedSerializer;
    private final ObjectMapper legacyObjectMapper;
    private final boolean legacyFallbackEnabled;

    public LegacyAwareJsonRedisSerializer(ObjectMapper objectMapper) {
        this(objectMapper, true);
    }

    public LegacyAwareJsonRedisSerializer(ObjectMapper objectMapper, boolean legacyFallbackEnabled) {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.tayota.operationservice.")
                .allowIfSubType("java.")
                .allowIfSubTypeIsArray()
                .build();
        this.typedSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .customize(JsonMapper.Builder::findAndAddModules)
                .build();
        this.legacyObjectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        this.legacyFallbackEnabled = legacyFallbackEnabled;
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
            if (!legacyFallbackEnabled) {
                throw exception;
            }
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
