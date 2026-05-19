package com.nguyendin.operationservice.repository;

import com.nguyendin.operationservice.entity.Appointment;
import com.nguyendin.operationservice.enums.AppointmentStatus;
import com.nguyendin.operationservice.enums.AppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByUserIdOrderByScheduledDateDesc(UUID userId);

    List<Appointment> findByStatusOrderByScheduledDateAsc(AppointmentStatus status);

    List<Appointment> findByTypeAndStatusOrderByScheduledDateAsc(
            AppointmentType type,
            AppointmentStatus status
    );

    List<Appointment> findByMechanicIdAndStatusInAndScheduledDateBetween(
            UUID mechanicId,
            List<AppointmentStatus> statuses,
            Instant start,
            Instant end
    );

    List<Appointment> findByAssistantIdAndStatusInAndScheduledDateBetween(
            UUID assistantId,
            List<AppointmentStatus> statuses,
            Instant start,
            Instant end
    );

    List<Appointment> findByDealershipIdAndTypeAndStatusInAndScheduledDateBetween(
            UUID dealershipId,
            AppointmentType type,
            List<AppointmentStatus> statuses,
            Instant start,
            Instant end
    );
}