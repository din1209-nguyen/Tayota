package com.nguyendin.carservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record UserBehaviorLogEvent(
        @JsonProperty("user_id")
        UUID userId,
        @JsonProperty("action_type")
        String actionType,
        String description,
        @JsonProperty("created_at")
        Instant createdAt
) {
}
