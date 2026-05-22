package com.tayota.userservice.repository;

import com.tayota.userservice.entity.Appointment;
import com.tayota.userservice.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    // Dùng để lấy tất cả appointment của user đang đăng nhập
    // Sắp xếp theo lịch mới nhất
    List<Appointment> findByUserIdOrderByScheduledStartAtDesc(UUID userId);

}
