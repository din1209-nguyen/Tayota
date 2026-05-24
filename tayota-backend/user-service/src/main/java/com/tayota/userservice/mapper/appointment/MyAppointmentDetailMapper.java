package com.tayota.userservice.mapper.appointment;

import com.tayota.userservice.config.AppointmentBookingProperties;
import com.tayota.userservice.dto.Response.appointment.MyAppointmentDetailResponse;
import com.tayota.userservice.entity.appointment.Appointment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

// Mapper để chuyển đổi Appointment entity sang DTO chi tiết dành cho user.
@Component
@RequiredArgsConstructor
public class MyAppointmentDetailMapper {
    private final AppointmentBookingProperties bookingProperties;

    public MyAppointmentDetailResponse toResponse(Appointment appointment) {
        ZonedDateTime start = appointment.getScheduledStartAt()
                .atZone(bookingProperties.getBusinessZone());

        ZonedDateTime end = appointment.getScheduledEndAt()
                .atZone(bookingProperties.getBusinessZone());

        return new MyAppointmentDetailResponse(
                appointment.getId(),
                appointment.getType(),
                appointment.getStatus(),
                start.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime(),
                appointment.getDealershipId(),
                appointment.getCarVersionId(),
                appointment.getVinId(),
                appointment.getNotes(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
