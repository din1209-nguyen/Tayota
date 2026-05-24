package com.tayota.userservice.service.appointment;

import com.tayota.userservice.config.AppointmentBookingProperties;
import com.tayota.userservice.enums.appointment.AppointmentStatus;
import com.tayota.userservice.repository.appointment.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// Scheduler định kỳ chạy để tự động chuyển các lịch hẹn đã qua ngày hẹn 1 ngày nhưng khách chưa check-in sang trạng thái EXPIRED.
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentExpirationScheduler {
    private static final List<AppointmentStatus> EXPIRABLE_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED
    );

    private final AppointmentRepository appointmentRepository;
    private final AppointmentBookingProperties bookingProperties;

    // Định kỳ chuyển các lịch đã qua ngày hẹn 1 ngày nhưng khách chưa check-in sang EXPIRED.
    // Ví dụ: lịch ngày 24 chưa đến thì sang ngày 26 mới tự hết hạn.
    @Transactional
    @Scheduled(
            cron = "${appointment.expiration.cron:0 10 0 * * *}",
            zone = "${appointment.booking.business-zone:Asia/Bangkok}"
    )
    public void expirePastAppointments() {
        Instant now = Instant.now();
        Instant expirationCutoff = now.atZone(bookingProperties.getBusinessZone())
                .toLocalDate()
                .minusDays(1)
                .atStartOfDay(bookingProperties.getBusinessZone())
                .toInstant();

        int expiredCount = appointmentRepository.expireAppointmentsPastScheduledEnd(
                EXPIRABLE_STATUSES,
                AppointmentStatus.EXPIRED,
                now,
                expirationCutoff
        );

        if (expiredCount > 0) {
            log.info("Expired {} appointments before cutoff {}", expiredCount, expirationCutoff);
        }
    }
}
