package com.tayota.operationservice.repository.appointment;

import com.tayota.operationservice.entity.appointment.ServiceTimeSlot;
import com.tayota.operationservice.enums.appointment.AppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceTimeSlotRepository extends JpaRepository<ServiceTimeSlot, UUID> {

    // Lấy tất cả khung giờ dịch vụ của đại lý, sắp xếp theo loại cuộc hẹn từ A-Z, sau đó sắp xếp theo giờ bắt đầu từ sớm nhất đến muộn nhất
    List<ServiceTimeSlot> findByDealershipIdOrderByAppointmentTypeAscStartTimeAsc(UUID dealershipId);

    // Lấy tất cả khung giờ dịch vụ của đại lý theo loại cuộc hẹn, chỉ lấy các khung giờ đang hoạt động, sắp xếp theo giờ bắt đầu từ sớm nhất đến muộn nhất
    List<ServiceTimeSlot> findByDealershipIdAndAppointmentTypeAndActiveTrueOrderByStartTimeAsc(
            UUID dealershipId,
            AppointmentType appointmentType
    );

    // Lấy một khung giờ dịch vụ của đại lý theo loại cuộc hẹn và giờ bắt đầu, chỉ lấy khi khung giờ đang hoạt động
    Optional<ServiceTimeSlot> findByDealershipIdAndAppointmentTypeAndStartTimeAndActiveTrue(
            UUID dealershipId,
            AppointmentType appointmentType,
            LocalTime startTime
    );
}
