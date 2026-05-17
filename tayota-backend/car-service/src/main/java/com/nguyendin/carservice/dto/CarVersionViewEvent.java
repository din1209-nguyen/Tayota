package com.nguyendin.carservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record CarVersionViewEvent(
        @JsonProperty("user_id")
        UUID userId,
        @JsonProperty("car_version_id")
        UUID carVersionId,
        @JsonProperty("viewed_at")
        Instant viewedAt
) {
}
