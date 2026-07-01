package com.tayota.operationservice.service.appointment;

import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.util.SecurityContextUtil;
import com.tayota.operationservice.config.AppointmentBookingProperties;
import com.tayota.operationservice.dto.request.appointment.CreateAppointmentHolidayRequest;
import com.tayota.operationservice.dto.request.appointment.CreateServiceTimeSlotRequest;
import com.tayota.operationservice.dto.request.appointment.UpdateAppointmentHolidayRequest;
import com.tayota.operationservice.dto.request.appointment.UpdateServiceTimeSlotRequest;
import com.tayota.operationservice.dto.response.appointment.AppointmentAvailableSlotsResponse;
import com.tayota.operationservice.dto.response.appointment.AppointmentCalendarDayResponse;
import com.tayota.operationservice.dto.response.appointment.AppointmentHolidayResponse;
import com.tayota.operationservice.dto.response.appointment.ServiceTimeSlotResponse;
import com.tayota.operationservice.entity.appointment.AppointmentHoliday;
import com.tayota.operationservice.entity.user.ServiceAdvisor;
import com.tayota.operationservice.entity.appointment.ServiceTimeSlot;
import com.tayota.operationservice.enums.appointment.AppointmentType;
import com.tayota.operationservice.repository.appointment.AppointmentHolidayRepository;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.appointment.ServiceTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentScheduleService {

    private final ServiceTimeSlotRepository serviceTimeSlotRepository;
    private final AppointmentHolidayRepository appointmentHolidayRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final AppointmentBookingProperties bookingProperties;

    // ============================== DÀNH CHO USER ĐẶT LỊCH ==============================

    // Kiểm tra xem ngày hẹn, giờ hẹn có hợp lệ hay không, nếu hợp lệ thì trả về khoảng thời gian bắt đầu và kết thúc của cuộc hẹn
    // Các điều kiện chính: ngày nằm trong khoảng cho phép, ngày không phải ngày nghỉ của đại lý, và khung giờ tồn tại trong database.
    @Transactional(readOnly = true)
    public AppointmentTimeRange validateAppointmentTimeRange(
            UUID dealershipId,
            AppointmentType appointmentType,
            LocalDate appointmentDate,
            LocalTime startTime
    ) {
        // Kiểm tra tính hợp lệ của ngày hẹn
        validateBookingDate(appointmentDate);

        // Kiểm tra xem ngày hẹn có rơi vào ngày nghỉ của đại lý hay không
        AppointmentHoliday holiday = appointmentHolidayRepository
                .findByDealershipIdAndHolidayDateAndActiveTrue(dealershipId, appointmentDate)
                .orElse(null);

        if (holiday != null) {
            throw new CustomException(400, "Đại lý nghỉ vào ngày này, vui lòng chọn ngày khác");
        }

        // Kiểm tra xem khung giờ hẹn có hợp lệ hay không
        ServiceTimeSlot slot = serviceTimeSlotRepository
                .findByDealershipIdAndAppointmentTypeAndStartTimeAndActiveTrue(dealershipId, appointmentType, startTime)
                .orElseThrow(() -> new CustomException(400, "Khung giờ không hợp lệ"));

        // Kiểm tra xem giờ bắt đầu và giờ kết thúc của khung giờ hẹn có hợp lệ hay không
        validateSlotTime(slot.getStartTime(), slot.getEndTime());

        // Nếu tất cả kiểm tra đều hợp lệ, trả về khoảng thời gian bắt đầu và kết thúc của cuộc hẹn
        Instant startAt = buildSlotStartAt(appointmentDate, slot);
        Instant endAt = buildSlotEndAt(appointmentDate, slot);

        validateMinimumNotice(startAt);

        return new AppointmentTimeRange(startAt, endAt);
    }


    // ================================== DÀNH CHO CỐ VẤN DỊCH VỤ QUẢN LÝ KHUNG GIỜ  ==============================

    // Lấy tất cả khung giờ hẹn có sẵn cho một ngày hẹn và loại cuộc hẹn nhất định
    // Nếu ngày hẹn rơi vào ngày nghỉ thì trả về thông tin ngày nghỉ và danh sách khung giờ trống
    @Transactional(readOnly = true)
    public AppointmentAvailableSlotsResponse getAvailableSlots(UUID dealershipId, AppointmentType appointmentType, LocalDate appointmentDate) {
        validateBookingDate(appointmentDate);

        // Kiểm tra xem ngày hẹn có rơi vào ngày nghỉ của đại lý hay không
        AppointmentHoliday holiday = appointmentHolidayRepository
                .findByDealershipIdAndHolidayDateAndActiveTrue(dealershipId, appointmentDate)
                .orElse(null);

        // Nếu ngày hẹn rơi vào ngày nghỉ, trả về thông tin ngày nghỉ và danh sách khung giờ trống (rỗng)
        if (holiday != null) {
            return new AppointmentAvailableSlotsResponse(appointmentDate, true, holiday.getReason(), List.of());
        }

        // Nếu ngày hẹn không rơi vào ngày nghỉ, trả về danh sách khung giờ trống cho ngày hẹn và loại cuộc hẹn đã chọn
        List<AppointmentAvailableSlotsResponse.SlotItem> slots = serviceTimeSlotRepository
                .findByDealershipIdAndAppointmentTypeAndActiveTrueOrderByStartTimeAsc(dealershipId, appointmentType)
                .stream()
                .filter(slot -> isAfterMinimumNotice(appointmentDate, slot))
                .map(this::toAvailableSlotItem)
                .toList();

        return new AppointmentAvailableSlotsResponse(appointmentDate, false, null, slots);
    }

    @Transactional(readOnly = true)
    public List<AppointmentCalendarDayResponse> getAvailabilityCalendar(
            UUID dealershipId,
            AppointmentType appointmentType,
            LocalDate from,
            LocalDate to
    ) {
        LocalDate today = LocalDate.now(bookingProperties.getBusinessZone());
        LocalDate maxDate = today.plusMonths(4);
        LocalDate startDate = from == null || from.isBefore(today) ? today : from;
        LocalDate endDate = to == null ? startDate.plusDays(34) : to;

        if (startDate.isAfter(maxDate)) {
            throw new CustomException(400, "Khoảng ngày vượt quá giới hạn đặt lịch");
        }

        if (endDate.isBefore(startDate)) {
            throw new CustomException(400, "Khoảng ngày không hợp lệ");
        }

        if (endDate.isAfter(maxDate)) {
            endDate = maxDate;
        }

        if (startDate.plusDays(62).isBefore(endDate)) {
            endDate = startDate.plusDays(62);
        }

        List<ServiceTimeSlot> activeSlots = serviceTimeSlotRepository
                .findByDealershipIdAndAppointmentTypeAndActiveTrueOrderByStartTimeAsc(dealershipId, appointmentType);

        LocalDate finalEndDate = endDate;
        return startDate.datesUntil(finalEndDate.plusDays(1))
                .map(date -> toCalendarDay(dealershipId, date, activeSlots))
                .toList();
    }

    // Lấy tất cả khung giờ của đại lý mà cố vấn dịch vụ hiện tại đang quản lý.
    // Danh sách này dùng cho màn hình quản lý khung giờ, bao gồm cả khung giờ đang bật và đã tắt.
    @Transactional(readOnly = true)
    public List<ServiceTimeSlotResponse> getMyDealershipTimeSlots() {
        UUID dealershipId = getCurrentServiceAdvisorDealershipId();

        return serviceTimeSlotRepository.findByDealershipIdOrderByAppointmentTypeAscStartTimeAsc(dealershipId)
                .stream()
                .map(this::toTimeSlotResponse)
                .toList();
    }

    // Tạo một khung giờ mới cho đại lý của cố vấn dịch vụ hiện tại.
    // Backend tự lấy dealershipId từ tài khoản đang đăng nhập để tránh việc tạo khung giờ cho đại lý khác.
    @Transactional
    public ServiceTimeSlotResponse createTimeSlot(CreateServiceTimeSlotRequest request) {
        UUID dealershipId = getCurrentServiceAdvisorDealershipId();
        validateSlotTime(request.getStartTime(), request.getEndTime());
        boolean active = request.getActive() == null || request.getActive();
        if (active) {
            validateNoOverlappingActiveSlot(
                    dealershipId,
                    request.getAppointmentType(),
                    request.getStartTime(),
                    request.getEndTime(),
                    null
            );
        }

        ServiceTimeSlot slot = ServiceTimeSlot.builder()
                .dealershipId(dealershipId)
                .appointmentType(request.getAppointmentType())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .active(active)
                .build();

        ServiceTimeSlot saved = serviceTimeSlotRepository.saveAndFlush(slot);

        return toTimeSlotResponse(saved);
    }

    // Cập nhật thông tin một khung giờ đã có, như loại lịch, giờ bắt đầu, giờ kết thúc hoặc trạng thái active.
    // Chỉ cho phép sửa khung giờ thuộc đúng đại lý của cố vấn dịch vụ hiện tại.
    @Transactional
    public ServiceTimeSlotResponse updateTimeSlot(UUID slotId, UpdateServiceTimeSlotRequest request) {
        UUID dealershipId = getCurrentServiceAdvisorDealershipId();
        ServiceTimeSlot slot = serviceTimeSlotRepository.findById(slotId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy khung giờ"));

        validateBelongsToDealership(slot.getDealershipId(), dealershipId);

        if (request.getAppointmentType() != null) {
            slot.setAppointmentType(request.getAppointmentType());
        }

        if (request.getStartTime() != null) {
            slot.setStartTime(request.getStartTime());
        }

        if (request.getEndTime() != null) {
            slot.setEndTime(request.getEndTime());
        }

        if (request.getActive() != null) {
            slot.setActive(request.getActive());
        }

        validateSlotTime(slot.getStartTime(), slot.getEndTime());
        if (Boolean.TRUE.equals(slot.getActive())) {
            validateNoOverlappingActiveSlot(
                    dealershipId,
                    slot.getAppointmentType(),
                    slot.getStartTime(),
                    slot.getEndTime(),
                    slot.getId()
            );
        }

        ServiceTimeSlot saved = serviceTimeSlotRepository.save(slot);

        return toTimeSlotResponse(saved);
    }

    // Xóa mềm một khung giờ bằng cách chuyển active = false.
    // Không xóa cứng khỏi database để giữ lại lịch sử cấu hình và tránh mất dữ liệu liên quan.
    @Transactional
    public void deleteTimeSlot(UUID slotId) {
        UUID dealershipId = getCurrentServiceAdvisorDealershipId();
        ServiceTimeSlot slot = serviceTimeSlotRepository.findById(slotId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy khung giờ"));

        validateBelongsToDealership(slot.getDealershipId(), dealershipId);
        slot.setActive(false);

        serviceTimeSlotRepository.save(slot);
    }


    // ============================== DÀNH CHO CỐ VẤN DỊCH VỤ QUẢN LÝ NGÀY NGHỈ  ==============================

    // Lấy tất cả ngày nghỉ của đại lý mà cố vấn dịch vụ hiện tại đang quản lý.
    // Danh sách này dùng cho màn hình quản lý ngày nghỉ.
    @Transactional(readOnly = true)
    public List<AppointmentHolidayResponse> getMyDealershipHolidays() {
        UUID dealershipId = getCurrentServiceAdvisorDealershipId();

        return appointmentHolidayRepository.findByDealershipIdOrderByHolidayDateDesc(dealershipId)
                .stream()
                .map(this::toHolidayResponse)
                .toList();
    }

    // Tạo ngày nghỉ mới cho đại lý của cố vấn dịch vụ hiện tại.
    // Khi ngày nghỉ đang active, user sẽ không thể đặt lịch vào ngày đó.
    @Transactional
    public AppointmentHolidayResponse createHoliday(CreateAppointmentHolidayRequest request) {
        UUID dealershipId = getCurrentServiceAdvisorDealershipId();
        boolean active = request.getActive() == null || request.getActive();
        if (active && appointmentHolidayRepository.findByDealershipIdAndHolidayDateAndActiveTrue(dealershipId, request.getHolidayDate()).isPresent()) {
            throw new CustomException(409, "Ngày nghỉ này đã tồn tại");
        }

        AppointmentHoliday holiday = AppointmentHoliday.builder()
                .dealershipId(dealershipId)
                .holidayDate(request.getHolidayDate())
                .reason(normalize(request.getReason()))
                .active(active)
                .build();

        AppointmentHoliday saved = appointmentHolidayRepository.saveAndFlush(holiday);

        return toHolidayResponse(saved);
    }

    // Cập nhật ngày nghỉ đã có, bao gồm ngày, lý do và trạng thái active.
    // Chỉ cho phép sửa ngày nghỉ thuộc đúng đại lý của cố vấn dịch vụ hiện tại.
    @Transactional
    public AppointmentHolidayResponse updateHoliday(UUID holidayId, UpdateAppointmentHolidayRequest request) {
        UUID dealershipId = getCurrentServiceAdvisorDealershipId();
        AppointmentHoliday holiday = appointmentHolidayRepository.findById(holidayId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy ngày nghỉ"));

        validateBelongsToDealership(holiday.getDealershipId(), dealershipId);

        if (request.getHolidayDate() != null) {
            holiday.setHolidayDate(request.getHolidayDate());
        }

        if (request.getReason() != null) {
            holiday.setReason(normalize(request.getReason()));
        }

        if (request.getActive() != null) {
            holiday.setActive(request.getActive());
        }

        if (Boolean.TRUE.equals(holiday.getActive())) {
            appointmentHolidayRepository.findByDealershipIdAndHolidayDateAndActiveTrue(dealershipId, holiday.getHolidayDate())
                    .filter(existing -> !existing.getId().equals(holiday.getId()))
                    .ifPresent(existing -> {
                        throw new CustomException(409, "Ngày nghỉ này đã tồn tại");
                    });
        }

        AppointmentHoliday saved = appointmentHolidayRepository.save(holiday);

        return toHolidayResponse(saved);
    }

    // Xóa mềm một ngày nghỉ bằng cách chuyển active = false.
    // Sau khi tắt active, ngày đó không còn chặn user đặt lịch nữa.
    @Transactional
    public void deleteHoliday(UUID holidayId) {
        UUID dealershipId = getCurrentServiceAdvisorDealershipId();
        AppointmentHoliday holiday = appointmentHolidayRepository.findById(holidayId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy ngày nghỉ"));

        validateBelongsToDealership(holiday.getDealershipId(), dealershipId);
        holiday.setActive(false);
        appointmentHolidayRepository.save(holiday);
    }

    // ============================== HÀM HỖ TRỢ CHUNG ==============================

    // Chuyển entity ServiceTimeSlot thành item trả về cho frontend trong API available-slots.
    // Hiện tại available luôn là true vì không còn kiểm tra trùng lịch.
    private AppointmentAvailableSlotsResponse.SlotItem toAvailableSlotItem(ServiceTimeSlot slot) {
        return new AppointmentAvailableSlotsResponse.SlotItem(
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                true
        );
    }

    private AppointmentCalendarDayResponse toCalendarDay(UUID dealershipId, LocalDate date, List<ServiceTimeSlot> activeSlots) {
        AppointmentHoliday holiday = appointmentHolidayRepository
                .findByDealershipIdAndHolidayDateAndActiveTrue(dealershipId, date)
                .orElse(null);

        if (holiday != null) {
            return new AppointmentCalendarDayResponse(date, true, holiday.getReason(), false);
        }

        boolean hasAvailableSlots = activeSlots.stream().anyMatch(slot -> isAfterMinimumNotice(date, slot));

        return new AppointmentCalendarDayResponse(date, false, null, hasAvailableSlots);
    }

    // Kiểm tra ngày hẹn có hợp lệ không: bắt buộc có ngày, không ở quá khứ và không quá 4 tháng.
    // Điều kiện tối thiểu 12 tiếng được kiểm tra theo từng khung giờ cụ thể.
    private void validateBookingDate(LocalDate appointmentDate) {
        if (appointmentDate == null) {
            throw new CustomException(400, "Vui lòng chọn ngày hẹn");
        }

        LocalDate today = LocalDate.now(bookingProperties.getBusinessZone());
        LocalDate maxDate = today.plusMonths(4);

        if (appointmentDate.isBefore(today) || appointmentDate.isAfter(maxDate)) {
            throw new CustomException(400, "Chỉ được đặt lịch từ hôm nay đến tối đa 4 tháng tiếp theo");
        }
    }

    // Kiểm tra khung giờ bắt đầu có cách thời điểm hiện tại tối thiểu theo cấu hình hay không.
    private void validateMinimumNotice(Instant startAt) {
        if (startAt.isBefore(getMinimumAllowedStartAt())) {
            throw new CustomException(400, "Chỉ được đặt lịch trước tối thiểu 12 tiếng");
        }
    }

    // Dùng cho API available-slots để chỉ trả về những khung giờ FE được phép hiển thị cho user chọn.
    private boolean isAfterMinimumNotice(LocalDate appointmentDate, ServiceTimeSlot slot) {
        return !buildSlotStartAt(appointmentDate, slot).isBefore(getMinimumAllowedStartAt());
    }

    // Hàm này tính toán thời điểm bắt đầu tối thiểu được phép đặt lịch từ thời điểm hiện tại.
    private Instant getMinimumAllowedStartAt() {
        return ZonedDateTime.now(bookingProperties.getBusinessZone())
                .plus(bookingProperties.getMinimumNotice())
                .toInstant();
    }

    // Hàm này xây dựng thời điểm bắt đầu của cuộc hẹn dựa trên ngày hẹn và khung giờ đã chọn, chuyển sang Instant để lưu vào database.
    private Instant buildSlotStartAt(LocalDate appointmentDate, ServiceTimeSlot slot) {
        return appointmentDate.atTime(slot.getStartTime())
                .atZone(bookingProperties.getBusinessZone())
                .toInstant();
    }

    // Hàm này xây dựng thời điểm kết thúc của cuộc hẹn dựa trên ngày hẹn và khung giờ đã chọn, chuyển sang Instant để lưu vào database.
    private Instant buildSlotEndAt(LocalDate appointmentDate, ServiceTimeSlot slot) {
        return appointmentDate.atTime(slot.getEndTime())
                .atZone(bookingProperties.getBusinessZone())
                .toInstant();
    }

    // Kiểm tra giờ bắt đầu và giờ kết thúc của khung giờ.
    // Giờ kết thúc bắt buộc phải sau giờ bắt đầu.
    private void validateSlotTime(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new CustomException(400, "Vui lòng nhập đầy đủ giờ bắt đầu và giờ kết thúc");
        }

        if (!endTime.isAfter(startTime)) {
            throw new CustomException(400, "Giờ kết thúc phải sau giờ bắt đầu");
        }
    }

    // Chặn tạo hoặc bật khung giờ đang hoạt động bị trùng/chồng lấn trong cùng đại lý và cùng loại lịch.
    private void validateNoOverlappingActiveSlot(
            UUID dealershipId,
            AppointmentType appointmentType,
            LocalTime startTime,
            LocalTime endTime,
            UUID excludedSlotId
    ) {
        serviceTimeSlotRepository.findByDealershipIdAndAppointmentTypeAndActiveTrueOrderByStartTimeAsc(dealershipId, appointmentType)
                .stream()
                .filter(slot -> excludedSlotId == null || !excludedSlotId.equals(slot.getId()))
                .filter(slot -> startTime.isBefore(slot.getEndTime()) && endTime.isAfter(slot.getStartTime()))
                .findFirst()
                .ifPresent(slot -> {
                    throw new CustomException(409, "Khung giờ này bị trùng với khung giờ đang hoạt động");
                });
    }

    // Lấy dealershipId của cố vấn dịch vụ đang đăng nhập từ SecurityContext.
    // Hàm này giúp các API quản lý chỉ thao tác trên dữ liệu của đúng đại lý.
    private UUID getCurrentServiceAdvisorDealershipId() {
        UUID currentUserId = UUID.fromString(SecurityContextUtil.getCurrentUserId());

        ServiceAdvisor serviceAdvisor = serviceAdvisorRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(403, "Tài khoản cố vấn dịch vụ chưa được gán đại lý"));

        return serviceAdvisor.getDealershipId();
    }

    // Kiểm tra dữ liệu cần sửa có thuộc đại lý của cố vấn dịch vụ hiện tại không.
    // Nếu không trùng đại lý thì từ chối để tránh sửa dữ liệu của nơi khác.
    private void validateBelongsToDealership(UUID ownerDealershipId, UUID currentDealershipId) {
        if (!ownerDealershipId.equals(currentDealershipId)) {
            throw new CustomException(403, "Bạn không có quyền chỉnh sửa dữ liệu của đại lý này");
        }
    }

    // Chuyển entity ServiceTimeSlot thành response trả về cho các API quản lý khung giờ.
    private ServiceTimeSlotResponse toTimeSlotResponse(ServiceTimeSlot slot) {
        return new ServiceTimeSlotResponse(
                slot.getId(),
                slot.getAppointmentType(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getActive()
        );
    }

    // Chuyển entity AppointmentHoliday thành response trả về cho các API quản lý ngày nghỉ.
    private AppointmentHolidayResponse toHolidayResponse(AppointmentHoliday holiday) {
        return new AppointmentHolidayResponse(
                holiday.getId(),
                holiday.getHolidayDate(),
                holiday.getReason(),
                holiday.getActive()
        );
    }

    // Chuẩn hóa chuỗi không bắt buộc: cắt khoảng trắng, nếu rỗng thì trả về null.
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // Record nhỏ để trả về thời điểm bắt đầu và kết thúc sau khi đã validate lịch hẹn.
    public record AppointmentTimeRange(Instant startAt, Instant endAt) {
    }
}
