package com.tayota.userservice.dto.Response.workorder;

import com.tayota.userservice.enums.workorder.ServiceTicketStatus;
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
    private ServiceTicketStatus status;
    private Instant receivingAt;
    private BigDecimal totalAmount;
}
