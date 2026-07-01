package com.tayota.operationservice.dto.response.report;

import java.math.BigDecimal;

public record AdvisorReportSummaryResponse(
        long totalAppointments,
        long completedAppointments,
        long canceledOrExpiredAppointments,
        long serviceTickets,
        BigDecimal completedServiceRevenue,
        BigDecimal averageRating
) {
}
