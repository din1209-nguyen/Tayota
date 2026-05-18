package com.tayota.carservice.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarVersionViewEvent {
    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("car_version_id")
    private UUID carVersionId;

    @JsonProperty("viewed_at")
    private Instant viewedAt;
}
