package com.tayota.userservice.repository;

import com.tayota.userservice.entity.Appointment;
import com.tayota.userservice.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByUserIdOrderByScheduledDateDesc(UUID userId);

    List<Appointment> findByMechanicIdAndStatusInAndScheduledDateBetween(
            UUID mechanicId,
            List<AppointmentStatus> statuses,
            Instant start,
            Instant end
    );
}