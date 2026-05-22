package com.tayota.userservice.dto.Response.appointment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AppointmentHolidayResponse {
    private UUID id;
    private LocalDate holidayDate;
    private String reason;
}
