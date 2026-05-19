package com.nguyendin.operationservice.repository;

import com.nguyendin.operationservice.entity.AppointmentReminder;
import com.nguyendin.operationservice.enums.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentReminderRepository extends JpaRepository<AppointmentReminder, UUID> {
    List<AppointmentReminder> findByStatusAndRemindAtLessThanEqual(
            ReminderStatus status,
            Instant now
    );

    List<AppointmentReminder> findByAppointmentIdAndStatus(
            UUID appointmentId,
            ReminderStatus status
    );

    List<AppointmentReminder> findByAppointmentId(UUID appointmentId);
}