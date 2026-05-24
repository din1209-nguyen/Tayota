package com.tayota.operationservice.dto.Response.appointment;

import com.tayota.operationservice.enums.appointment.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ServiceTimeSlotResponse {
    private UUID id;
    private AppointmentType appointmentType;
    private LocalTime startTime;
    private LocalTime endTime;
}
