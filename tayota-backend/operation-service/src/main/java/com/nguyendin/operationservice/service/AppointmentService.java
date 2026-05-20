package com.nguyendin.operationservice.service;

import com.nguyendin.operationservice.dto.request.CreateServiceAppointmentRequest;
import com.nguyendin.operationservice.dto.request.CreateTestDriveAppointmentRequest;
import com.nguyendin.operationservice.dto.response.AppointmentResponse;
import com.nguyendin.operationservice.entity.Appointment;
import com.nguyendin.operationservice.entity.GuestInformation;
import com.nguyendin.operationservice.enums.AppointmentStatus;
import com.nguyendin.operationservice.enums.AppointmentType;
import com.nguyendin.operationservice.mapper.AppointmentMapper;
import com.nguyendin.operationservice.repository.AppointmentRepository;
import com.nguyendin.operationservice.repository.GuestInformationRepository;
import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private static final Duration APPOINTMENT_SLOT = Duration.ofHours(1);

    private final AppointmentRepository appointmentRepository;
    private final GuestInformationRepository guestInformationRepository;

    @Transactional
    public AppointmentResponse createTestDriveAppointment(CreateTestDriveAppointmentRequest request, UUID userId) {
        UUID dealershipId = parseUuid(request.getDealershipId(), "Đại lý không hợp lệ");
        UUID carVersionId = parseUuid(request.getCarVersionId(), "Phiên bản xe không hợp lệ");
        UUID mechanicId = parseOptionalUuid(request.getMechanicId(), "Kỹ thuật viên không hợp lệ");

        GuestInformation guestInformation = createGuestInformationIfNeeded(
                userId,
                request.getGuestFullName(),
                request.getGuestEmail(),
                request.getGuestPhone()
        );

        if (mechanicId != null) {
            validateMechanicAvailable(mechanicId, request.getScheduledDate());
        }

        Appointment appointment = Appointment.builder()
                .userId(userId)
                .guestInformation(guestInformation)
                .carVersionId(carVersionId)
                .dealershipId(dealershipId)
                .mechanicId(mechanicId)
                .type(AppointmentType.TEST_DRIVE)
                .status(AppointmentStatus.PENDING)
                .scheduledDate(request.getScheduledDate())
                .notes(normalize(request.getNotes()))
                .build();

        return AppointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse createServiceAppointment(CreateServiceAppointmentRequest request, UUID userId) {
        if (request.getType() == AppointmentType.TEST_DRIVE) {
            throw new CustomException(400, "Loại lịch hẹn không hợp lệ cho dịch vụ sửa chữa/bảo dưỡng");
        }

        UUID dealershipId = parseUuid(request.getDealershipId(), "Đại lý không hợp lệ");
        UUID carVersionId = parseOptionalUuid(request.getCarVersionId(), "Phiên bản xe không hợp lệ");
        UUID mechanicId = parseOptionalUuid(request.getMechanicId(), "Kỹ thuật viên không hợp lệ");

        GuestInformation guestInformation = createGuestInformationIfNeeded(
                userId,
                request.getGuestFullName(),
                request.getGuestEmail(),
                request.getGuestPhone()
        );

        if (mechanicId != null) {
            validateMechanicAvailable(mechanicId, request.getScheduledDate());
        }

        Appointment appointment = Appointment.builder()
                .userId(userId)
                .guestInformation(guestInformation)
                .vinId(normalize(request.getVinId()))
                .carVersionId(carVersionId)
                .dealershipId(dealershipId)
                .mechanicId(mechanicId)
                .type(request.getType())
                .status(AppointmentStatus.PENDING)
                .scheduledDate(request.getScheduledDate())
                .notes(normalize(request.getNotes()))
                .build();

        return AppointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(UUID userId) {
        if (userId == null) {
            throw new CustomException(401, "Vui lòng đăng nhập để xem lịch hẹn");
        }

        return appointmentRepository.findByUserIdOrderByScheduledDateDesc(userId)
                .stream()
                .map(AppointmentMapper::toResponse)
                .toList();
    }

    private GuestInformation createGuestInformationIfNeeded(UUID userId, String fullName, String email, String phone) {
        if (userId != null) {
            return null;
        }

        if (!StringUtils.hasText(fullName) || !StringUtils.hasText(email) || !StringUtils.hasText(phone)) {
            throw new CustomException(400, "Khách vãng lai cần nhập họ tên, email và số điện thoại");
        }

        GuestInformation guestInformation = GuestInformation.builder()
                .fullName(fullName.trim())
                .email(email.trim())
                .phone(phone.trim())
                .build();

        return guestInformationRepository.save(guestInformation);
    }

    private void validateMechanicAvailable(UUID mechanicId, Instant scheduledDate) {
        List<AppointmentStatus> activeStatuses = List.of(
                AppointmentStatus.PENDING,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.CHECKED_IN,
                AppointmentStatus.IN_PROGRESS
        );

        Instant start = scheduledDate.minus(APPOINTMENT_SLOT);
        Instant end = scheduledDate.plus(APPOINTMENT_SLOT);

        boolean busy = !appointmentRepository
                .findByMechanicIdAndStatusInAndScheduledDateBetween(mechanicId, activeStatuses, start, end)
                .isEmpty();

        if (busy) {
            throw new CustomException(409, "Kỹ thuật viên đã có lịch trong khung giờ này");
        }
    }


    private UUID parseUuid(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new CustomException(400, message);
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new CustomException(400, message);
        }
    }

    private UUID parseOptionalUuid(String value, String message) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return parseUuid(value, message);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}