package com.tayota.operationservice.dto.response.workorder;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ServiceInvoiceResponse {
    private String invoiceCode;
    private Instant issuedAt;
    private UUID serviceTicketId;
    private UUID dealershipId;
    private String dealershipName;
    private String dealershipAddress;
    private String customerType;
    private String customerFullName;
    private String customerEmail;
    private String customerPhone;
    private String vinId;
    private UUID carVersionId;
    private String carVersionName;
    private UUID mechanicId;
    private String mechanicName;
    private Integer mileageAtService;
    private String vehicleCondition;
    private List<ServiceItemResponse> items;
    private BigDecimal totalAmount;
}
