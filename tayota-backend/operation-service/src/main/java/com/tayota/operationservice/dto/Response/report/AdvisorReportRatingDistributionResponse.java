package com.tayota.operationservice.dto.response.report;

public record AdvisorReportRatingDistributionResponse(
        int rating,
        long count
) {
}
