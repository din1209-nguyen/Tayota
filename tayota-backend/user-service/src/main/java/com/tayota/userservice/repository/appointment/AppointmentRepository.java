package com.tayota.userservice.repository.appointment;

import com.tayota.userservice.entity.appointment.Appointment;
import com.tayota.userservice.enums.appointment.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    // Dùng để lấy tất cả appointment của user đang đăng nhập
    // Sắp xếp theo lịch mới nhất
    List<Appointment> findByUserIdOrderByScheduledStartAtDesc(UUID userId);


    // Dùng để lấy tất cả appointment của một đại lý, không phân biệt trạng thái
    // Sáp xếp theo lịch mới nhất
    @Query("""
        select appointment
        from Appointment appointment
        where appointment.dealershipId = :dealershipId
        order by appointment.scheduledStartAt desc
        """)
    List<Appointment> findByDealershipIdOrderByScheduledStartAtDesc(
            @Param("dealershipId") UUID dealershipId
    );

    // Dùng để lấy tất cả appointment của một đại lý theo trạng thái, mặc định controller sẽ truyền PENDING
    // Sáp xếp theo lịch mới nhất
    @Query("""
        select appointment
        from Appointment appointment
        where appointment.status = :status
          and appointment.dealershipId = :dealershipId
        order by appointment.scheduledStartAt desc
        """)
    List<Appointment> findByStatusAndDealershipIdOrderByScheduledStartAtDesc(
            @Param("status") AppointmentStatus status,
            @Param("dealershipId") UUID dealershipId
    );

    // Dùng cho user đã đăng nhập xem chi tiết appointment của chính mình
    @Query("""
            select appointment
            from Appointment appointment
            where appointment.id = :id
              and appointment.userId = :userId
            """)
    Optional<Appointment> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    // Tự động chuyển các lịch đã quá giờ hẹn nhưng chưa check-in sang EXPIRED.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Appointment appointment
            set appointment.status = :expiredStatus,
                appointment.expiredAt = :expiredAt,
                appointment.updatedAt = :expiredAt
            where appointment.status in :statuses
              and appointment.scheduledEndAt < :now
            """)
    int expireAppointmentsPastScheduledEnd(
            @Param("statuses") Collection<AppointmentStatus> statuses,
            @Param("expiredStatus") AppointmentStatus expiredStatus,
            @Param("expiredAt") Instant expiredAt,
            @Param("now") Instant now
    );

}
