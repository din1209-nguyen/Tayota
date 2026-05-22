package com.tayota.userservice.mapper;


import com.tayota.userservice.config.AppointmentBookingProperties;
import com.tayota.userservice.dto.Response.AppointmentCreatedResponse;
import com.tayota.userservice.entity.Appointment;
import com.tayota.userservice.entity.GuestInformation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

// Mapper để chuyển đổi giữa Appointment entity và AppointmentCreatedResponse DTO sau khi tạo cuộc hẹn thành công
@Component
@RequiredArgsConstructor
public class AppointmentCreatedMapper {
    private final AppointmentBookingProperties bookingProperties;

    public AppointmentCreatedResponse toResponse(Appointment appointment) {
        GuestInformation guest = appointment.getGuestInformation();

        ZonedDateTime start = appointment.getScheduledStartAt()
                .atZone(bookingProperties.getBusinessZone());

        ZonedDateTime end = appointment.getScheduledEndAt()
                .atZone(bookingProperties.getBusinessZone());

        return new AppointmentCreatedResponse(
                appointment.getId(),
                appointment.getType(),
                appointment.getStatus(),
                start.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime()
        );
    }
}
