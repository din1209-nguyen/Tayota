package com.tayota.operationservice.service.review;

import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.util.SecurityContextUtil;
import com.tayota.operationservice.dto.request.review.CreateCustomerReviewRequest;
import com.tayota.operationservice.dto.response.review.CustomerReviewResponse;
import com.tayota.operationservice.entity.appointment.Appointment;
import com.tayota.operationservice.entity.appointment.GuestInformation;
import com.tayota.operationservice.entity.review.CustomerReview;
import com.tayota.operationservice.entity.workorder.Mechanic;
import com.tayota.operationservice.entity.workorder.ServiceTicket;
import com.tayota.operationservice.enums.appointment.AppointmentStatus;
import com.tayota.operationservice.enums.appointment.AppointmentType;
import com.tayota.operationservice.enums.review.ReviewStatus;
import com.tayota.operationservice.enums.review.ReviewType;
import com.tayota.operationservice.enums.workorder.ServiceTicketStatus;
import com.tayota.operationservice.repository.appointment.AppointmentRepository;
import com.tayota.operationservice.repository.review.CustomerReviewRepository;
import com.tayota.operationservice.repository.workorder.MechanicRepository;
import com.tayota.operationservice.repository.workorder.ServiceTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Service chính để quản lý đánh giá của khách hàng sau khi hoàn thành lịch hẹn hoặc phiếu dịch vụ
// Bao gồm tạo đánh giá pending sau khi hoàn thành, lấy thông tin đánh giá qua token, và submit đánh giá.
@Service
@RequiredArgsConstructor
public class CustomerReviewService {
    private static final Duration REVIEW_TOKEN_TTL = Duration.ofDays(14);

    private final AppointmentRepository appointmentRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final CustomerReviewRepository customerReviewRepository;
    private final MechanicRepository mechanicRepository;

    // Đánh giá lịch hẹn sau khi hoàn thành.
    // Chỉ dành cho khách hàng đã đăng nhập.
    // Guest đánh giá bằng link token qua /reviews/token/{token}.
    @Transactional
    public CustomerReviewResponse reviewAppointment(UUID appointmentId, CreateCustomerReviewRequest request) {
        UUID userId = getCurrentUserId();

        // Kiểm tra xem lịch hẹn có tồn tại và thuộc về user đang đăng nhập hay không
        Appointment appointment = appointmentRepository.findByIdAndUserId(appointmentId, userId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy lịch hẹn cần đánh giá"));

        // Lấy đánh giá liên quan đến lịch hẹn này thông qua review token.
        CustomerReview review = customerReviewRepository.findByReviewToken(createPendingReviewForAppointment(appointment))
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy đánh giá cần cập nhật"));

        return submitReview(review, request);
    }

    // Đánh giá phiếu dịch vụ sau khi hoàn thành.
    // Chỉ dành cho khách hàng đã đăng nhập.
    // Guest đánh giá bằng link token qua /reviews/token/{token}.
    @Transactional
    public CustomerReviewResponse reviewServiceTicket(UUID serviceTicketId, CreateCustomerReviewRequest request) {
        UUID userId = getCurrentUserId();

        // Kiểm tra xem phiếu dịch vụ có tồn tại và thuộc về user đang đăng nhập hay không
        ServiceTicket serviceTicket = serviceTicketRepository.findById(serviceTicketId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiếu dịch vụ cần đánh giá"));

        // Kiểm tra quyền truy cập: chỉ có user tạo lịch hẹn hoặc phiếu dịch vụ mới được phép đánh giá
        if (!userId.equals(serviceTicket.getUserId())) {
            throw new CustomException(403, "Bạn không có quyền đánh giá phiếu dịch vụ này");
        }

        // Lấy đánh giá liên quan đến phiếu dịch vụ này thông qua review token.
        CustomerReview review = customerReviewRepository.findByReviewToken(createPendingReviewForServiceTicket(serviceTicket))
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy đánh giá cần cập nhật"));

        return submitReview(review, request);
    }

    // Lấy danh sách đánh giá của user đang đăng nhập, bao gồm cả đánh giá về lịch hẹn và phiếu dịch vụ, sắp xếp theo thời gian tạo mới nhất.
    @Transactional(readOnly = true)
    public List<CustomerReviewResponse> getMyReviews() {
        UUID userId = getCurrentUserId();

        return customerReviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerReviewResponse> getMyMechanicReviews() {
        UUID mechanicId = getCurrentUserId();

        return customerReviewRepository.findByMechanicIdOrderByCreatedAtDesc(mechanicId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Tạo một đánh giá pending mới cho một lịch hẹn đã hoàn thành. Nếu đã tồn tại đánh giá cho lịch hẹn này, trả về token của đánh giá đó.
    @Transactional
    public String createPendingReviewForAppointment(Appointment appointment) {
        validateCompletedAppointment(appointment);

        // Kiểm tra nếu đã tồn tại đánh giá cho lịch hẹn này, nếu có thì trả về token của đánh giá đó để tránh tạo nhiều đánh giá cho cùng một lịch hẹn.
        if (customerReviewRepository.existsByAppointment_Id(appointment.getId())) {
            return customerReviewRepository.findByReviewTokenForAppointment(appointment.getId())
                    .orElseThrow(() -> new CustomException(400, "Lịch hẹn này đã có đánh giá"));
        }

        // Nếu lịch hẹn là dịch vụ, lấy phiếu dịch vụ liên quan để liên kết với đánh giá.
        // Nếu không phải dịch vụ hoặc không tìm thấy phiếu dịch vụ, để null.
        ServiceTicket serviceTicket = appointment.getType() == AppointmentType.SERVICE
                ? serviceTicketRepository.findByAppointmentId(appointment.getId()).orElse(null)
                : null;

        CustomerReview review = CustomerReview.builder()
                .reviewType(appointment.getType() == AppointmentType.SERVICE ? ReviewType.SERVICE : ReviewType.TEST_DRIVE)
                .status(ReviewStatus.PENDING)
                .reviewToken(UUID.randomUUID().toString())
                .tokenExpiresAt(Instant.now().plus(REVIEW_TOKEN_TTL))
                .appointment(appointment)
                .serviceTicket(serviceTicket)
                .userId(appointment.getUserId())
                .guestFullName(appointment.getGuestInformation() == null ? null : appointment.getGuestInformation().getFullName())
                .guestEmail(appointment.getGuestInformation() == null ? null : appointment.getGuestInformation().getEmail())
                .guestPhone(appointment.getGuestInformation() == null ? null : appointment.getGuestInformation().getPhone())
                .dealershipId(appointment.getDealershipId())
                .mechanicId(resolveMechanicId(appointment, serviceTicket))
                .build();

        return customerReviewRepository.save(review).getReviewToken();
    }

    // Tạo một đánh giá pending mới cho một phiếu dịch vụ đã hoàn thành.
    // Nếu đã tồn tại đánh giá cho phiếu dịch vụ này, trả về token của đánh giá đó.
    @Transactional
    public String createPendingReviewForServiceTicket(ServiceTicket serviceTicket) {
        // Kiểm tra xem phiếu dịch vụ có thuộc về lịch hẹn đã hoàn thành hay không, nếu không thì không cho phép tạo đánh giá.
        validateCompletedServiceTicket(serviceTicket);

        // Kiểm tra nếu đã tồn tại đánh giá cho phiếu dịch vụ này
        // Nếu có thì trả về token của đánh giá đó để tránh tạo nhiều đánh giá cho cùng một phiếu dịch vụ.
        if (customerReviewRepository.existsByServiceTicket_Id(serviceTicket.getId())) {
            return customerReviewRepository.findByReviewTokenForServiceTicket(serviceTicket.getId())
                    .orElseThrow(() -> new CustomException(400, "Phiếu dịch vụ này đã có đánh giá"));
        }

        // Lấy lịch hẹn liên quan đến phiếu dịch vụ để lấy thông tin khách hàng và đại lý.
        Appointment appointment = serviceTicket.getAppointment();

        // Lấy thông tin khách hàng từ lịch hẹn, ưu tiên lấy từ guestInformation vì có thể khách vãng lai không có userId.
        GuestInformation guest = serviceTicket.getGuestInformation();

        // Nếu không có guestInformation, thử lấy thông tin từ userId. Nếu userId cũng null hoặc không tìm thấy thông tin, trả về contact rỗng.
        CustomerReview review = CustomerReview.builder()
                .reviewType(ReviewType.SERVICE)
                .status(ReviewStatus.PENDING)
                .reviewToken(UUID.randomUUID().toString())
                .tokenExpiresAt(Instant.now().plus(REVIEW_TOKEN_TTL))
                .appointment(appointment)
                .serviceTicket(serviceTicket)
                .userId(serviceTicket.getUserId())
                .guestFullName(guest == null ? null : guest.getFullName())
                .guestEmail(guest == null ? null : guest.getEmail())
                .guestPhone(guest == null ? null : guest.getPhone())
                .dealershipId(serviceTicket.getDealershipId())
                .mechanicId(serviceTicket.getMechanicId())
                .build();

        return customerReviewRepository.save(review).getReviewToken();
    }
    // Lấy thông tin đánh giá qua token
    // chỉ trả về thông tin nếu token hợp lệ và đánh giá chưa hết hạn.
    // Nếu token không hợp lệ hoặc đánh giá đã hết hạn, trả về lỗi tương ứng.
    @Transactional(readOnly = true)
    public CustomerReviewResponse getReviewByToken(String token) {
        CustomerReview review = getReviewByTokenOrThrow(token);

        return toResponse(review);
    }

    // Submit đánh giá qua token, chỉ cho phép submit nếu token hợp lệ và đánh giá chưa hết hạn.
    @Transactional
    public CustomerReviewResponse submitReviewByToken(String token, CreateCustomerReviewRequest request) {
        CustomerReview review = getReviewByTokenOrThrow(token);

        return submitReview(review, request);
    }

    // Hàm chính để xử lý việc submit đánh giá, bao gồm validate dữ liệu, cập nhật trạng thái đánh giá, và tính toán lại điểm trung bình của thợ sửa nếu cần.
    private CustomerReviewResponse submitReview(CustomerReview review, CreateCustomerReviewRequest request) {
        validateReviewCanSubmit(review);

        // Nếu là đánh giá lái thử, không cho phép có đánh giá thợ sửa, nếu request có dữ liệu đánh giá thợ sửa thì trả về lỗi.
        if (review.getReviewType() == ReviewType.TEST_DRIVE) {
            validateNoMechanicReviewForTestDrive(request);
        }

        // Cập nhật thông tin đánh giá từ request, bao gồm điểm và bình luận về dịch vụ, cũng như điểm và bình luận về thợ sửa nếu có.
        review.setServiceRating(request.getServiceRating());
        review.setServiceComment(normalize(request.getServiceComment()));

        // Chỉ cập nhật đánh giá thợ sửa nếu là đánh giá dịch vụ, đánh giá lái thử không có phần đánh giá thợ sửa.
        if (review.getReviewType() == ReviewType.SERVICE) {
            review.setMechanicRating(request.getMechanicRating());
            review.setMechanicComment(normalize(request.getMechanicComment()));
        }

        // Cập nhật trạng thái đánh giá thành SUBMITTED và gán thời điểm gửi đánh giá là thời điểm hiện tại.
        review.setStatus(ReviewStatus.SUBMITTED);
        review.setSubmittedAt(Instant.now());

        // Lưu đánh giá vào database và cập nhật điểm trung bình của thợ sửa nếu đánh giá có liên kết với thợ sửa cụ thể.
        CustomerReview saved = customerReviewRepository.save(review);
        updateMechanicAverageRatingIfNeeded(saved.getMechanicId());

        return toResponse(saved);
    }

    // Hàm validate để kiểm tra xem đánh giá có thể submit hay không, bao gồm kiểm tra trạng thái đánh giá và thời điểm hết hạn của token.
    private void validateReviewCanSubmit(CustomerReview review) {
        if (review.getStatus() == ReviewStatus.SUBMITTED) {
            throw new CustomException(400, "Đánh giá này đã được gửi");
        }

        if (review.getStatus() == ReviewStatus.EXPIRED || review.getTokenExpiresAt().isBefore(Instant.now())) {
            review.setStatus(ReviewStatus.EXPIRED);
            customerReviewRepository.save(review);
            throw new CustomException(400, "Link đánh giá đã hết hạn");
        }
    }

    // Hàm lấy đánh giá theo token, nếu token không hợp lệ hoặc đánh giá đã hết hạn thì trả về lỗi tương ứng.
    private CustomerReview getReviewByTokenOrThrow(String token) {
        if (!StringUtils.hasText(token)) {
            throw new CustomException(400, "Link đánh giá không hợp lệ");
        }

        return customerReviewRepository.findByReviewToken(token.trim())
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy link đánh giá"));
    }

    // Hàm validate để kiểm tra xem lịch hẹn đã hoàn thành hay chưa, chỉ cho phép tạo đánh giá nếu lịch hẹn đã hoàn thành, nếu không thì trả về lỗi.
    private void validateCompletedAppointment(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new CustomException(400, "Chỉ có thể tạo đánh giá cho lịch hẹn đã hoàn thành");
        }
    }

    // Hàm validate để kiểm tra xem phiếu dịch vụ đã hoàn thành hay chưa, chỉ cho phép tạo đánh giá nếu phiếu dịch vụ đã hoàn thành, nếu không thì trả về lỗi.
    private void validateCompletedServiceTicket(ServiceTicket serviceTicket) {
        if (serviceTicket.getStatus() != ServiceTicketStatus.COMPLETED) {
            throw new CustomException(400, "Chỉ có thể tạo đánh giá cho phiếu dịch vụ đã hoàn thành");
        }
    }

    // Hàm validate để kiểm tra nếu là đánh giá lái thử thì không được phép có đánh giá thợ sửa, nếu request có dữ liệu đánh giá thợ sửa thì trả về lỗi.
    private void validateNoMechanicReviewForTestDrive(CreateCustomerReviewRequest request) {
        if (request.getMechanicRating() != null || StringUtils.hasText(request.getMechanicComment())) {
            throw new CustomException(400, "Lịch lái thử không có đánh giá thợ sửa");
        }
    }

    // Hàm để xác định mechanicId liên quan đến đánh giá, ưu tiên lấy từ phiếu dịch vụ nếu có
    // Nếu không có thì lấy từ lịch hẹn.
    // Điều này giúp đảm bảo rằng đánh giá được liên kết chính xác với thợ sửa xe đã phục vụ khách hàng.
    private UUID resolveMechanicId(Appointment appointment, ServiceTicket serviceTicket) {
        if (serviceTicket != null && serviceTicket.getMechanicId() != null) {
            return serviceTicket.getMechanicId();
        }

        return appointment.getMechanicId();
    }

    // Hàm để cập nhật điểm trung bình của thợ sửa nếu đánh giá có liên kết với thợ sửa cụ thể.
    private void updateMechanicAverageRatingIfNeeded(UUID mechanicId) {
        if (mechanicId == null) {
            return;
        }

        Double averageRating = customerReviewRepository.findAverageMechanicRating(mechanicId);
        if (averageRating == null) {
            return;
        }

        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElse(null);

        if (mechanic == null) {
            return;
        }

        mechanic.setAverageRating(BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP));
        mechanicRepository.save(mechanic);
    }

    // Hàm để chuyển đổi từ entity CustomerReview sang DTO CustomerReviewResponse
    // giúp tách biệt giữa tầng dữ liệu và tầng trình bày, đồng thời chỉ trả về những thông tin cần thiết cho client.
    private CustomerReviewResponse toResponse(CustomerReview review) {
        return new CustomerReviewResponse(
                review.getId(),
                review.getReviewType(),
                review.getStatus(),
                review.getAppointment() == null ? null : review.getAppointment().getId(),
                review.getServiceTicket() == null ? null : review.getServiceTicket().getId(),
                review.getDealershipId(),
                review.getServiceRating(),
                resolveVinId(review),
                review.getGuestFullName(),
                review.getGuestEmail(),
                review.getServiceComment(),
                review.getMechanicId(),
                review.getMechanicRating(),
                review.getMechanicComment(),
                review.getTokenExpiresAt(),
                review.getSubmittedAt(),
                review.getCreatedAt()
        );
    }

    private String resolveVinId(CustomerReview review) {
        if (review.getServiceTicket() != null) {
            return review.getServiceTicket().getVinId();
        }

        if (review.getAppointment() != null) {
            return review.getAppointment().getVinId();
        }

        return null;
    }

    // Hàm để chuẩn hóa chuỗi đầu vào, loại bỏ khoảng trắng thừa và trả về null nếu chuỗi rỗng hoặc chỉ chứa khoảng trắng.
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private UUID getCurrentUserId() {
        return UUID.fromString(SecurityContextUtil.getCurrentUserId());
    }
}
