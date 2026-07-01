package com.tayota.operationservice.dto.response.review;

import java.math.BigDecimal;

public record AdvisorReviewSummaryResponse(
        BigDecimal serviceAverageRating,
        BigDecimal testDriveAverageRating,
        long submittedServiceCount,
        long submittedTestDriveCount
) {
}
