package com.tayota.operationservice.controller.report;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.response.report.AdvisorReportResponse;
import com.tayota.operationservice.service.report.AdvisorReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class AdvisorReportController {
    private final AdvisorReportService advisorReportService;

    @GetMapping("/advisor")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AdvisorReportResponse> getAdvisorReport(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "ALL") String type
    ) {
        AdvisorReportResponse response = advisorReportService.getAdvisorReport(from, to, type);

        return ApiResponse.success(200, "Lấy báo cáo đại lý thành công!", response);
    }
}
