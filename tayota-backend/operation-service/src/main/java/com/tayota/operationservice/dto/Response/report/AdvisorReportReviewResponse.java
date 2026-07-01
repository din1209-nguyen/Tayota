package com.tayota.operationservice.dto.response.report;

import java.time.Instant;
import java.util.UUID;

public record AdvisorReportReviewResponse(
        UUID id,
        String reviewType,
        UUID appointmentId,
        UUID serviceId,
        String customerFullName,
        Integer serviceRating,
        String serviceComment,
        Instant submittedAt
) {
}
