package com.nguyendin.carservice.dto;

import java.util.UUID;

public record CarSeriesResponse(
        UUID id,
        String name,
        String description,
        CarStyleResponse style
) {
}
