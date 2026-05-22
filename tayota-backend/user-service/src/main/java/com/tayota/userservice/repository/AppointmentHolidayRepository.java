package com.tayota.userservice.repository;

import com.tayota.userservice.entity.AppointmentHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentHolidayRepository extends JpaRepository<AppointmentHoliday, UUID> {

    // Kiểm tra xem đại lý có ngày nghỉ nào vào ngày được chỉ định hay không, chỉ trả về các ngày nghỉ đang hoạt động
    Optional<AppointmentHoliday> findByDealershipIdAndHolidayDateAndActiveTrue(UUID dealershipId, LocalDate holidayDate);

    // Lấy tất cả ngày nghỉ của đại lý, sắp xếp theo ngày nghỉ từ mới nhất đến cũ nhất
    List<AppointmentHoliday> findByDealershipIdOrderByHolidayDateDesc(UUID dealershipId);
}
