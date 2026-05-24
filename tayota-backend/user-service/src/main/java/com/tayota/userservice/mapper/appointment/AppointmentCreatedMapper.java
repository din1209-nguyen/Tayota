package com.tayota.userservice.mapper.appointment;


import com.tayota.userservice.config.AppointmentBookingProperties;
import com.tayota.userservice.dto.Response.appointment.AppointmentCreatedResponse;
import com.tayota.userservice.entity.appointment.Appointment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

// Mapper để chuyển đổi giữa Appointment entity và AppointmentCreatedResponse DTO sau khi tạo cuộc hẹn thành công
@Component
@RequiredArgsConstructor
public class AppointmentCreatedMapper {
    private final AppointmentBookingProperties bookingProperties;

    public AppointmentCreatedResponse toResponse(Appointment appointment) {

        ZonedDateTime start = appointment.getScheduledStartAt()
                .atZone(bookingProperties.getBusinessZone());

        ZonedDateTime end = appointment.getScheduledEndAt()
                .atZone(bookingProperties.getBusinessZone());

        ZonedDateTime createdAt = appointment.getCreatedAt()
                .atZone(bookingProperties.getBusinessZone());

        return new AppointmentCreatedResponse(
                appointment.getId(),
                appointment.getType(),
                appointment.getStatus(),
                start.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime(),
                createdAt.toLocalDateTime()
        );
    }
}
