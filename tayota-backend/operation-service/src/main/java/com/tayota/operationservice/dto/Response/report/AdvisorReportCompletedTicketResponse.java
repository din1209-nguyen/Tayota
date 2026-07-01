package com.tayota.operationservice.dto.response.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdvisorReportCompletedTicketResponse(
        UUID id,
        String customerFullName,
        String vehicle,
        UUID mechanicId,
        String mechanicName,
        BigDecimal totalAmount,
        Instant completedAt
) {
}
