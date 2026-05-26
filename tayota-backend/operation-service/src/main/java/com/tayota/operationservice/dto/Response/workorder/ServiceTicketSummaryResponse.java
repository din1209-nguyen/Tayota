package com.tayota.operationservice.dto.response.workorder;

import com.tayota.operationservice.enums.workorder.ServiceTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ServiceTicketSummaryResponse {
    private UUID id;
    private UUID appointmentId;
    private UUID guestInformationId;
    private String vinId;
    private UUID dealershipId;
    private UUID mechanicId;
    private String customerType;
    private String customerFullName;
    private String customerEmail;
    private String customerPhone;
    private ServiceTicketStatus status;
    private Instant receivingAt;
    private BigDecimal totalAmount;
}
