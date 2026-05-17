package com.nguyendin.carservice.dto;

import java.util.UUID;

public record CarStyleResponse(
        UUID id,
        String name,
        String description
) {
}
