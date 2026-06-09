package com.tayota.operationservice.dto.response.report;

import java.util.List;

public record AdvisorReportResponse(
        AdvisorReportSummaryResponse summary,
        List<AdvisorReportStatusCountResponse> appointmentStatus,
        List<AdvisorReportStatusCountResponse> serviceTicketStatus,
        List<AdvisorReportRevenueByDayResponse> revenueByDay,
        List<AdvisorReportRatingDistributionResponse> ratingDistribution,
        List<AdvisorReportCompletedTicketResponse> recentCompletedServiceTickets,
        List<AdvisorReportReviewResponse> recentCustomerReviews
) {
}
