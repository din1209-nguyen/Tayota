package com.tayota.operationservice.dto.response.review;

import java.math.BigDecimal;

public record MechanicReviewSummaryResponse(
        BigDecimal averageMechanicRating,
        long submittedCount,
        long pendingCount,
        long totalCount
) {
}
