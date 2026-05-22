package com.tayota.userservice.service.appointment;

import com.tayota.userservice.config.AppointmentBookingProperties;
import com.tayota.userservice.entity.appointment.Appointment;
import com.tayota.userservice.entity.appointment.GuestInformation;
import com.tayota.userservice.enums.NotificationType;
import com.tayota.userservice.repository.UserProfileRepository;
import com.tayota.userservice.service.EmailService;
import com.tayota.userservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;

// Service chuyên xử lý các thông báo liên quan đến lịch hẹn, bao gồm notification trong app và email cho khách hàng.
@Service
@RequiredArgsConstructor
public class AppointmentNotificationService {
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserProfileRepository userProfileRepository;
    private final AppointmentBookingProperties bookingProperties;

    // Gửi thông báo xác nhận lịch hẹn sau khi cố vấn dịch vụ chuyển trạng thái sang CONFIRMED.
    // User đăng nhập sẽ nhận cả notification trong app và email; guest chỉ nhận email vì không có userId.
    public void notifyAppointmentConfirmed(Appointment appointment) {
        CustomerContact customer = buildCustomerContact(appointment);
        String title = "Tayota - Xác nhận lịch hẹn thành công";
        String content = buildConfirmationContent(appointment, customer.fullName());

        if (appointment.getUserId() != null) {
            notificationService.createNotification(
                    appointment.getUserId(),
                    null,
                    NotificationType.APPOINTMENT,
                    title,
                    content
            );
        }

        if (StringUtils.hasText(customer.email())) {
            emailService.sendEmailAsync(customer.email(), title, content);
        }
    }

    // Hàm dùng để xây dựng thông tin liên hệ của khách hàng
    private CustomerContact buildCustomerContact(Appointment appointment) {
        // Ưu tiên lấy thông tin từ guestInformation nếu có, vì có thể khách vãng lai không có userId.
        GuestInformation guest = appointment.getGuestInformation();

        if (guest != null) {
            return new CustomerContact(guest.getFullName(), guest.getEmail());
        }

        // Nếu không có guestInformation, thử lấy thông tin từ userId. Nếu userId cũng null hoặc không tìm thấy thông tin, trả về contact rỗng.
        if (appointment.getUserId() == null) {
            return new CustomerContact(null, null);
        }

        // Lấy thông tin liên hệ của user từ repository. Nếu không tìm thấy, trả về contact rỗng.
        return userProfileRepository.findContactByUserId(appointment.getUserId())
                .map(userContact -> new CustomerContact(userContact.getFullname(), userContact.getEmail()))
                .orElseGet(() -> new CustomerContact(null, null));
    }

    // Hàm xây dựng nội dung email/notification xác nhận lịch hẹn, có thể tùy chỉnh theo loại cuộc hẹn và thông tin khách hàng.
    private String buildConfirmationContent(Appointment appointment, String customerName) {
        ZonedDateTime start = appointment.getScheduledStartAt()
                .atZone(bookingProperties.getBusinessZone());

        ZonedDateTime end = appointment.getScheduledEndAt()
                .atZone(bookingProperties.getBusinessZone());

        String greetingName = StringUtils.hasText(customerName) ? customerName : "quý khách";
        String appointmentType = switch (appointment.getType()) {
            case SERVICE -> "bảo dưỡng/sửa chữa";
            case TEST_DRIVE -> "lái thử";
        };

        return """
                Xin chào %s,

                Lịch hẹn %s của bạn đã được cố vấn dịch vụ xác nhận.

                Thời gian: %s - %s, ngày %s

                Cảm ơn bạn đã sử dụng dịch vụ Tayota.
                """.formatted(
                greetingName,
                appointmentType,
                start.toLocalTime(),
                end.toLocalTime(),
                start.toLocalDate()
        );
    }

    private record CustomerContact(String fullName, String email) {
    }
}
