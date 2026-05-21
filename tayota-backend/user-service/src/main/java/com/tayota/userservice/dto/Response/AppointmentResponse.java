package com.tayota.userservice.dto.Response;


import com.tayota.userservice.enums.AppointmentStatus;
import com.tayota.userservice.enums.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AppointmentResponse {
    private UUID id;
    private UUID userId;
    private UUID guestInformationId;
    private String guestFullName;
    private String guestEmail;
    private String guestPhone;
    private UUID carVersionId;
    private String vinId;
    private UUID dealershipId;
    private UUID mechanicId;
    private AppointmentType type;
    private AppointmentStatus status;
    private Instant scheduledDate;
    private String notes;
    private Instant createdAt;
}