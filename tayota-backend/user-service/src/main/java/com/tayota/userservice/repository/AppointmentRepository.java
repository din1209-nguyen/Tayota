package com.tayota.userservice.repository;

import com.tayota.userservice.entity.Appointment;
import com.tayota.userservice.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    // Dùng để lấy tất cả appointment của user đang đăng nhập
    // Sắp xếp theo lịch mới nhất
    List<Appointment> findByUserIdOrderByScheduledStartAtDesc(UUID userId);

    // Dùng cho quản lý/admin lấy tất cả appointment, không lọc theo trạng thái
    // Sắp xếp theo lịch mới nhất
    @Query("""
            select appointment
            from Appointment appointment
            left join fetch appointment.guestInformation
            order by appointment.scheduledStartAt desc
            """)
    List<Appointment> findAllWithGuestInformationOrderByScheduledStartAtDesc();

    // Dùng cho quản lý/admin lấy danh sách appointment theo trạng thái, mặc định controller sẽ truyền PENDING
    // Sắp xếp theo lịch mới nhất
    @Query("""
            select appointment
            from Appointment appointment
            left join fetch appointment.guestInformation
            where appointment.status = :status
            order by appointment.scheduledStartAt desc
            """)
    List<Appointment> findByStatusWithGuestInformationOrderByScheduledStartAtDesc(@Param("status") AppointmentStatus status);

    // Dùng cho quản lý/ admin xem chi tiết bất cứ appointment nào
    @Query("""
            select appointment
            from Appointment appointment
            left join fetch appointment.guestInformation
            where appointment.id = :id
            """)
    Optional<Appointment> findWithGuestInformationById(@Param("id") UUID id);

    // Dùng cho user đã đăng nhập xem chi tiết appointment của chính mình
    @Query("""
            select appointment
            from Appointment appointment
            left join fetch appointment.guestInformation
            where appointment.id = :id
              and appointment.userId = :userId
            """)
    Optional<Appointment> findWithGuestInformationByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
