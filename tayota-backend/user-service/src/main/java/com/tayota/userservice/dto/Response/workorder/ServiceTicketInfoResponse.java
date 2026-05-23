package com.tayota.userservice.dto.Response.workorder;

import com.tayota.userservice.enums.workorder.ServiceTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ServiceTicketInfoResponse {
    private UUID id;
    private UUID appointmentId;
    private String vinId;
    private UUID mechanicId;
    private Integer mileageAtService;
    private ServiceTicketStatus status;
    private String vehicleCondition;
    private String notes;
    private Instant receivingAt;
    private Instant processingAt;
    private BigDecimal totalAmount;
}
