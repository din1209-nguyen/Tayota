package com.tayota.operationservice.dto.response.appointment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AppointmentCalendarDayResponse {
    private LocalDate date;
    private Boolean holiday;
    private String holidayReason;
    private Boolean hasAvailableSlots;
}
