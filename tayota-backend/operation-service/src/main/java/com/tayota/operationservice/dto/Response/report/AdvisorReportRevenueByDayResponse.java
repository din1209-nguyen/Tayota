package com.tayota.operationservice.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdvisorReportRevenueByDayResponse(
        LocalDate date,
        BigDecimal amount
) {
}
