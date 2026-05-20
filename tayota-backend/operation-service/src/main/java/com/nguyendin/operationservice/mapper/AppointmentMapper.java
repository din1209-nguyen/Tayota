package com.nguyendin.operationservice.mapper;

import com.nguyendin.operationservice.dto.response.AppointmentResponse;
import com.nguyendin.operationservice.entity.Appointment;
import com.nguyendin.operationservice.entity.GuestInformation;

public class AppointmentMapper {
    public static AppointmentResponse toResponse(Appointment appointment) {
        GuestInformation guest = appointment.getGuestInformation();

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getUserId(),
                guest != null ? guest.getId() : null,
                guest != null ? guest.getFullName() : null,
                guest != null ? guest.getEmail() : null,
                guest != null ? guest.getPhone() : null,
                appointment.getCarVersionId(),
                appointment.getVinId(),
                appointment.getDealershipId(),
                appointment.getMechanicId(),
                appointment.getType(),
                appointment.getStatus(),
                appointment.getScheduledDate(),
                appointment.getNotes(),
                appointment.getCreatedAt()
        );
    }
}