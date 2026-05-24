package com.tayota.operationservice.mapper.appointment;

import com.tayota.operationservice.config.AppointmentBookingProperties;
import com.tayota.operationservice.dto.response.appointment.MyAppointmentDetailResponse;
import com.tayota.operationservice.entity.appointment.Appointment;
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
