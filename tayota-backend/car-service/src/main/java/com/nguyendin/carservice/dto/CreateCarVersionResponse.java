package com.nguyendin.carservice.dto;

import java.util.UUID;

public record CreateCarVersionResponse(
        UUID id,
        String message
) {
}
