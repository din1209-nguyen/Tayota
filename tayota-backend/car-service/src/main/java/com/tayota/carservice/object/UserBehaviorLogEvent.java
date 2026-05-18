package com.tayota.carservice.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserBehaviorLogEvent {
    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("action_type")
    private String actionType;

    private String description;

    @JsonProperty("created_at")
    private Instant createdAt;
}
