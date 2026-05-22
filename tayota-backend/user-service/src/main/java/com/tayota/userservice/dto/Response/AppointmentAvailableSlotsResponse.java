package com.tayota.userservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AppointmentAvailableSlotsResponse {
    private LocalDate appointmentDate;
    private Boolean holiday;
    private String holidayReason;
    private List<SlotItem> slots;

    @Getter
    @AllArgsConstructor
    public static class SlotItem {
        private UUID id;
        private LocalTime startTime;
        private LocalTime endTime;
        private Boolean available;
    }
}
