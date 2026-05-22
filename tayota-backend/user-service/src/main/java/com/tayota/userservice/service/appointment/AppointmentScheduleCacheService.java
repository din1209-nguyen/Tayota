package com.tayota.userservice.service.appointment;

import com.tayota.userservice.entity.appointment.AppointmentHoliday;
import com.tayota.userservice.entity.appointment.ServiceTimeSlot;
import com.tayota.userservice.enums.appointment.AppointmentType;
import com.tayota.userservice.repository.appointment.AppointmentHolidayRepository;
import com.tayota.userservice.repository.appointment.ServiceTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Service bọc các truy vấn cấu hình lịch hẹn có thể cache được.
// Không cache response available-slots vì còn phụ thuộc thời điểm hiện tại và rule đặt trước tối thiểu.
@Service
@RequiredArgsConstructor
public class AppointmentScheduleCacheService {
    private final ServiceTimeSlotRepository serviceTimeSlotRepository;
    private final AppointmentHolidayRepository appointmentHolidayRepository;

    // Cache danh sách khung giờ đang active theo đại lý và loại lịch hẹn.
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "appointmentActiveSlots",
            key = "#dealershipId.toString() + ':' + #appointmentType.name()"
    )
    public List<ServiceTimeSlot> getActiveTimeSlots(UUID dealershipId, AppointmentType appointmentType) {
        return serviceTimeSlotRepository.findByDealershipIdAndAppointmentTypeAndActiveTrueOrderByStartTimeAsc(
                dealershipId,
                appointmentType
        );
    }

    // Cache một khung giờ cụ thể để validate khi user submit đặt lịch.
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "appointmentActiveSlotByStart",
            key = "#dealershipId.toString() + ':' + #appointmentType.name() + ':' + #startTime.toString()"
    )
    public Optional<ServiceTimeSlot> getActiveTimeSlotByStart(
            UUID dealershipId,
            AppointmentType appointmentType,
            LocalTime startTime
    ) {
        return serviceTimeSlotRepository.findByDealershipIdAndAppointmentTypeAndStartTimeAndActiveTrue(
                dealershipId,
                appointmentType,
                startTime
        );
    }

    // Cache ngày nghỉ active theo đại lý và ngày cụ thể.
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "appointmentActiveHoliday",
            key = "#dealershipId.toString() + ':' + #holidayDate.toString()"
    )
    public Optional<AppointmentHoliday> getActiveHoliday(UUID dealershipId, LocalDate holidayDate) {
        return appointmentHolidayRepository.findByDealershipIdAndHolidayDateAndActiveTrue(dealershipId, holidayDate);
    }

    // Xóa cache slot khi cố vấn tạo, sửa hoặc tắt khung giờ.
    @CacheEvict(cacheNames = {"appointmentActiveSlots", "appointmentActiveSlotByStart"}, allEntries = true)
    public void evictTimeSlotCaches() {
    }

    // Xóa cache ngày nghỉ khi cố vấn tạo, sửa hoặc tắt ngày nghỉ.
    @CacheEvict(cacheNames = "appointmentActiveHoliday", allEntries = true)
    public void evictHolidayCaches() {
    }
}
