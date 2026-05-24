package com.tayota.operationservice.service.appointment;

import com.tayota.operationservice.config.AppointmentBookingProperties;
import com.tayota.operationservice.entity.appointment.Appointment;
import com.tayota.operationservice.entity.appointment.GuestInformation;
import com.tayota.operationservice.enums.NotificationType;
import com.tayota.operationservice.repository.UserProfileRepository;
import com.tayota.operationservice.service.EmailService;
import com.tayota.operationservice.service.NotificationService;
import com.tayota.operationservice.service.review.CustomerReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final CustomerReviewService customerReviewService;

    @Value("${review.link-base-url:http://localhost:3000/reviews}")
    private String reviewLinkBaseUrl;

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

    // Gửi lời cảm ơn và lời mời đánh giá sau khi lịch hẹn hoàn thành.
    public void notifyAppointmentCompleted(Appointment appointment) {
        CustomerContact customer = buildCustomerContact(appointment);
        String title = "Tayota - Cảm ơn quý khách đã sử dụng dịch vụ";

        // Tạo một review pending mới cho lịch hẹn này và lấy token để xây dựng link đánh giá.
        // Link này sẽ được gửi trong email và notification để khách hàng có thể dễ dàng truy cập và đánh giá trải nghiệm của mình.
        String reviewToken = customerReviewService.createPendingReviewForAppointment(appointment);

        // Xây dựng link đánh giá dựa trên token vừa tạo.
        // Đảm bảo rằng mỗi khách hàng chỉ có thể đánh giá một lần cho mỗi lịch hẹn
        // Link đánh giá sẽ hết hạn sau một khoảng thời gian nhất định để tránh việc đánh giá cũ không còn phù hợp.
        String reviewLink = buildReviewLink(reviewToken);
        String content = buildCompletionContent(appointment, customer.fullName(), reviewLink);

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

    // Hàm xây dựng nội dung email/notification sau khi lịch hẹn hoàn thành, bao gồm lời cảm ơn và lời mời đánh giá trải nghiệm.
    private String buildCompletionContent(Appointment appointment, String customerName, String reviewLink) {
        String greetingName = StringUtils.hasText(customerName) ? customerName : "quý khách";
        String appointmentType = switch (appointment.getType()) {
            case SERVICE -> "Dịch vụ bảo dưỡng/sửa chữa";
            case TEST_DRIVE -> "Buổi lái thử";
        };

        return """
                Xin chào %s,

                Cảm ơn bạn đã tin tưởng Tayota. %s của bạn đã được hoàn tất.

                Rất mong bạn dành ít phút để đánh giá trải nghiệm, giúp Tayota tiếp tục cải thiện chất lượng phục vụ.

                Link đánh giá: %s

                Trân trọng,
                Tayota
                """.formatted(greetingName, appointmentType, reviewLink);
    }

    // Hàm xây dựng link đánh giá dựa trên token, đảm bảo rằng link có thể chứa thêm tham số nếu đã có query parameters trong base URL.
    private String buildReviewLink(String token) {
        String separator = reviewLinkBaseUrl.contains("?") ? "&" : "?";
        return reviewLinkBaseUrl + separator + "token=" + token;
    }

    private record CustomerContact(String fullName, String email) {
    }
}
