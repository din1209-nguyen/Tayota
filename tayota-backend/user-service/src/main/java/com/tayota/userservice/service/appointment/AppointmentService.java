package com.tayota.userservice.service.appointment;

import com.tayota.commoncore.exception.CustomException;
import com.tayota.commoncore.util.SecurityContextUtil;
import com.tayota.userservice.config.AppointmentBookingProperties;
import com.tayota.userservice.config.CarServiceProperties;
import com.tayota.userservice.dto.Request.appointment.CreateServiceAppointmentRequest;
import com.tayota.userservice.dto.Request.appointment.CreateTestDriveAppointmentRequest;
import com.tayota.userservice.dto.Request.appointment.UpdateAppointmentRequest;
import com.tayota.userservice.dto.Response.appointment.AppointmentCreatedResponse;
import com.tayota.userservice.dto.Response.appointment.AppointmentManagementDetailResponse;
import com.tayota.userservice.dto.Response.appointment.MyAppointmentDetailResponse;
import com.tayota.userservice.entity.appointment.Appointment;
import com.tayota.userservice.entity.appointment.GuestInformation;
import com.tayota.userservice.entity.ServiceAdvisor;
import com.tayota.userservice.enums.appointment.AppointmentStatus;
import com.tayota.userservice.enums.appointment.AppointmentType;
import com.tayota.userservice.mapper.appointment.AppointmentCreatedMapper;
import com.tayota.userservice.mapper.appointment.MyAppointmentDetailMapper;
import com.tayota.userservice.repository.appointment.AppointmentRepository;
import com.tayota.userservice.repository.appointment.GuestInformationRepository;
import com.tayota.userservice.repository.ServiceAdvisorRepository;
import com.tayota.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

// Dịch vụ quản lý lịch hẹn lái thử và dịch vụ, bao gồm cả việc tạo lịch hẹn mới và lấy danh sách lịch hẹn của user
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final GuestInformationRepository guestInformationRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final UserProfileRepository userProfileRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppointmentBookingProperties bookingProperties;
    private final CarServiceProperties carServiceProperties;
    private final AppointmentScheduleService appointmentScheduleService;
    private final AppointmentNotificationService appointmentNotificationService;
    private final AppointmentCreatedMapper appointmentCreatedMapper;
    private final MyAppointmentDetailMapper myAppointmentDetailMapper;

    // ========================== SERVICE CHO END-USER ==========================

    // Dịch vụ tạo lịch hẹn lái thử cho cả user đã đăng nhập và guest chưa đăng nhập, phân biệt bằng userId có null hay không
    @Transactional
    public AppointmentCreatedResponse createTestDriveAppointment(CreateTestDriveAppointmentRequest request, UUID userId, String clientIp) {
        // Kiểm tra đại lý có tồn tại không
        UUID dealershipId = parseUuid(request.getDealershipId(), "Đại lý không hợp lệ");

        // Kiểm tra phiên bản xe có tồn tại không
        UUID carVersionId = parseUuid(request.getCarVersionId(), "Phiên bản xe không hợp lệ");

        // Xây dựng khoảng thời gian hẹn hợp lệ
        AppointmentScheduleService.AppointmentTimeRange timeRange = appointmentScheduleService.validateAppointmentTimeRange(
                dealershipId,
                AppointmentType.TEST_DRIVE,
                request.getAppointmentDate(),
                request.getStartTime()
        );

        // Nếu là guest thì tạo thông tin khách, nếu là user đã đăng nhập thì bỏ qua phần này
        GuestInformation guestInformation = createGuestInformationIfNeeded(
                userId,
                request.getGuestFullName(),
                request.getGuestEmail(),
                request.getGuestPhone()
        );

        // Kiểm tra chính sách đặt lịch: giới hạn số lượng, khoảng cách giữa các lần đặt.
        validateCreatePolicy(userId, guestInformation, clientIp);

        // Tạo lịch hẹn mới với trạng thái PENDING
        Appointment appointment = Appointment.builder()
                .userId(userId)
                .guestInformation(guestInformation)
                .carVersionId(carVersionId)
                .dealershipId(dealershipId)
                .type(AppointmentType.TEST_DRIVE)
                .status(AppointmentStatus.PENDING)
                .scheduledStartAt(timeRange.startAt())
                .scheduledEndAt(timeRange.endAt())
                .notes(normalize(request.getNotes()))
                .createdAt(Instant.now())
                .build();

        // Lưu lịch hẹn vào database
        Appointment saved = appointmentRepository.save(appointment);

        // Đánh dấu đã sử dụng chính sách đặt lịch (để áp dụng giới hạn thời gian giữa các lần đặt, giới hạn số lượng trong ngày, v.v.)
        markCreatePolicy(userId, guestInformation, clientIp);

        return appointmentCreatedMapper.toResponse(saved);
    }

    // Dịch vụ tạo lịch hẹn dịch vụ cho cả user đã đăng nhập và guest chưa đăng nhập, phân biệt bằng userId có null hay không
    @Transactional
    public AppointmentCreatedResponse createServiceAppointment(CreateServiceAppointmentRequest request, UUID userId, String clientIp) {

        // Kiểm tra số VIN hợp lệ trong hệ thống
        String vinId = normalizeVin(request.getVinId());
        // TODO: Bật lại khi car-service có API kiểm tra VIN nội bộ ổn định.
        // validateVinExists(vinId);

        // Kiểm tra đại lý có tồn tại không
        UUID dealershipId = parseUuid(request.getDealershipId(), "Đại lý không hợp lệ");

        // Xây dựng khoảng thời gian hẹn hợp lệ
        AppointmentScheduleService.AppointmentTimeRange timeRange = appointmentScheduleService.validateAppointmentTimeRange(
                dealershipId,
                AppointmentType.SERVICE,
                request.getAppointmentDate(),
                request.getStartTime()
        );

        // Nếu là guest thì tạo thông tin khách, nếu là user đã đăng nhập thì bỏ qua phần này
        GuestInformation guestInformation = createGuestInformationIfNeeded(
                userId,
                request.getGuestFullName(),
                request.getGuestEmail(),
                request.getGuestPhone()
        );

        // Kiểm tra chính sách đặt lịch: giới hạn số lượng, khoảng cách giữa các lần đặt, trùng lịch, v.v.
        validateCreatePolicy(userId, guestInformation, clientIp);

        // Tạo lịch hẹn mới với trạng thái PENDING
        Appointment appointment = Appointment.builder()
                .userId(userId)
                .guestInformation(guestInformation)
                .vinId(vinId)
                .dealershipId(dealershipId)
                .type(AppointmentType.SERVICE)
                .status(AppointmentStatus.PENDING)
                .scheduledStartAt(timeRange.startAt())
                .scheduledEndAt(timeRange.endAt())
                .notes(normalizeRequired(request.getNotes(), "Vui lòng nhập mô tả tình trạng xe"))
                .createdAt(Instant.now())
                .build();

        // Lưu lịch hẹn vào database
        Appointment saved = appointmentRepository.saveAndFlush(appointment);

        // Đánh dấu đã sử dụng chính sách đặt lịch (để áp dụng giới hạn thời gian giữa các lần đặt, giới hạn số lượng trong ngày, v.v.)
        markCreatePolicy(userId, guestInformation, clientIp);

        return appointmentCreatedMapper.toResponse(saved);
    }

    // Lấy danh sách lịch hẹn của user đang đăng nhập, sắp xếp theo ngày hẹn mới nhất
    @Transactional(readOnly = true)
    public List<AppointmentCreatedResponse> getMyAppointments(UUID userId) {

        // Chỉ cho phép user đã đăng nhập mới được xem lịch hẹn của mình
        if (userId == null) {
            throw new CustomException(401, "Vui lòng đăng nhập để xem lịch hẹn");
        }

        return appointmentRepository.findByUserIdOrderByScheduledStartAtDesc(userId)
                .stream()
                .map(appointmentCreatedMapper::toResponse)
                .toList();
    }


    // Lấy chi tiết một lịch hẹn của user đang đăng nhập.
    @Transactional(readOnly = true)
    public MyAppointmentDetailResponse getMyAppointmentDetail(UUID appointmentId, UUID userId) {
        if (userId == null) {
            throw new CustomException(401, "Vui lòng đăng nhập để xem chi tiết lịch hẹn");
        }

        Appointment appointment = appointmentRepository.findByIdAndUserId(appointmentId, userId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy lịch hẹn của bạn"));

        return myAppointmentDetailMapper.toResponse(appointment);
    }


    // ========================= SERVICE CHO SERVICE ADVISOR =============================

    // Lấy danh sách lịch hẹn của đại lý mà cố vấn dịch vụ đang thuộc về.
    // Nếu status là ALL thì lấy tất cả trạng thái.
    @Transactional(readOnly = true)
    public List<AppointmentCreatedResponse> getAppointmentsForServiceAdvisor(String status, UUID serviceAdvisorId) {
        ServiceAdvisor serviceAdvisor = serviceAdvisorRepository.findById(serviceAdvisorId)
                .orElseThrow(() -> new CustomException(403, "Cố vấn dịch vụ không tồn tại"));

        UUID dealershipId = serviceAdvisor.getDealershipId();

        // Nếu status là ALL thì không lọc theo trạng thái, chỉ lọc theo đại lý của cố vấn dịch vụ.
        if ("ALL".equalsIgnoreCase(normalize(status))) {
            return appointmentRepository.findByDealershipIdOrderByScheduledStartAtDesc(dealershipId)
                    .stream()
                    .map(appointmentCreatedMapper::toResponse)
                    .toList();
        }

        // Phân tích trạng thái từ chuỗi đầu vào, nếu không hợp lệ thì ném lỗi 400
        AppointmentStatus searchStatus = parseAppointmentStatus(status);

        // Lấy danh sách lịch hẹn theo trạng thái và đại lý của cố vấn dịch vụ
        return appointmentRepository.findByStatusAndDealershipIdOrderByScheduledStartAtDesc(searchStatus, dealershipId)
                .stream()
                .map(appointmentCreatedMapper::toResponse)
                .toList();
    }


    // Lấy chi tiết lịch hẹn cho cố vấn dịch vụ xử lý. Cố vấn chỉ được xem lịch thuộc đại lý mình.
    @Transactional(readOnly = true)
    public AppointmentManagementDetailResponse getAppointmentDetailForManagement(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy lịch hẹn"));

        validateCurrentUserCanHandleAppointment(appointment);

        return toManagementDetailResponse(appointment);
    }

    // Cập nhật thông tin lịch hẹn cho cố vấn dịch vụ. Cố vấn chỉ được cập nhật lịch thuộc đại lý mình.
    @Transactional
    public AppointmentManagementDetailResponse updateAppointmentForManagement(UUID appointmentId, UpdateAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy lịch hẹn"));
        AppointmentStatus oldStatus = appointment.getStatus();

        // Kiểm tra xem user hiện tại có quyền xử lý lịch hẹn này hay không (cố vấn dịch vụ chỉ được xử lý lịch của đại lý mình)
        validateCurrentUserCanHandleAppointment(appointment);

        // Cập nhật các trường thông tin khác nếu có thay đổi
        updateAppointmentFieldsIfNeeded(appointment, request);

        // Cập nhật ngày hẹn và giờ bắt đầu nếu có thay đổi
        updateScheduleIfNeeded(appointment, request);

        // Cập nhật trạng thái lịch hẹn nếu có thay đổi
        updateAppointmentStatusIfNeeded(appointment, request);

        // Lưu các thay đổi vào database

        Appointment saved = appointmentRepository.save(appointment);

        // Nếu trạng thái thay đổi sang CONFIRMED thì gửi thông báo xác nhận lịch hẹn cho khách hàng
        if (oldStatus != AppointmentStatus.CONFIRMED && saved.getStatus() == AppointmentStatus.CONFIRMED) {
            appointmentNotificationService.notifyAppointmentConfirmed(saved);
        }

        return toManagementDetailResponse(saved);
    }

    // ========================== CÁC HÀM TIỆN ÍCH CHUNG CHO SERVICE ==========================

    // Hàm kiểm tra xem user hiện tại có quyền xử lý lịch hẹn này hay không (cố vấn dịch vụ chỉ được xử lý lịch của đại lý mình), nếu không có quyền thì ném lỗi 403 với thông điệp tùy chỉnh
    private void validateCurrentUserCanHandleAppointment(Appointment appointment) {

        UUID dealershipId = getCurrentServiceAdvisorDealershipId();

        if (!appointment.getDealershipId().equals(dealershipId)) {
            throw new CustomException(403, "Bạn không có quyền xử lý lịch hẹn của đại lý này");
        }
    }

    // Hàm lấy dealershipId của cố vấn dịch vụ hiện tại từ JWT, nếu không tìm thấy cố vấn dịch vụ hoặc cố vấn dịch vụ không có đại lý thì ném lỗi 403 với thông điệp tùy chỉnh
    private UUID getCurrentServiceAdvisorDealershipId() {
        UUID currentUserId = UUID.fromString(SecurityContextUtil.getCurrentUserId());

        ServiceAdvisor serviceAdvisor = serviceAdvisorRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(403, "Tài khoản cố vấn dịch vụ chưa được gán đại lý"));

        return serviceAdvisor.getDealershipId();
    }

    // Hàm cập nhật ngày hẹn và giờ bắt đầu nếu có thay đổi, đồng thời kiểm tra tính hợp lệ của chúng, nếu chỉ thay đổi một trong hai trường này mà không có trường còn lại thì ném lỗi 400 với thông điệp tùy chỉnh
    private void updateScheduleIfNeeded(Appointment appointment, UpdateAppointmentRequest request) {
        if (request.getAppointmentDate() == null && request.getStartTime() == null) {
            return;
        }

        if (request.getAppointmentDate() == null || request.getStartTime() == null) {
            throw new CustomException(400, "Vui lòng nhập đầy đủ ngày hẹn và giờ bắt đầu");
        }

        AppointmentScheduleService.AppointmentTimeRange timeRange = appointmentScheduleService.validateAppointmentTimeRange(
                appointment.getDealershipId(),
                appointment.getType(),
                request.getAppointmentDate(),
                request.getStartTime()
        );
        appointment.setScheduledStartAt(timeRange.startAt());
        appointment.setScheduledEndAt(timeRange.endAt());
    }

    // Hàm cập nhật các trường thông tin khác của lịch hẹn nếu có thay đổi, bao gồm đại lý, ghi chú, lý do hủy, v.v.
    private void updateAppointmentFieldsIfNeeded(Appointment appointment, UpdateAppointmentRequest request) {
        // Cập nhật đại lý nếu có thay đổi
        if (StringUtils.hasText(request.getDealershipId())) {
            appointment.setDealershipId(parseUuid(request.getDealershipId(), "Đại lý không hợp lệ"));
        }

        // Cập nhật ghi chú nếu có thay đổi
        if (request.getNotes() != null) {
            appointment.setNotes(normalize(request.getNotes()));
        }

        // Cập nhật lý do hủy nếu có thay đổi
        if (request.getCancelReason() != null) {
            appointment.setCancelReason(normalize(request.getCancelReason()));
        }
    }

    // Hàm cập nhật trạng thái lịch hẹn nếu có thay đổi, đồng thời cập nhật các trường thời gian tương ứng với từng trạng thái (confirmedAt, completedAt, canceledAt, expiredAt), nếu chuyển sang trạng thái CANCELED mà không có lý do hủy thì ném lỗi 400 với thông điệp tùy chỉnh
    private void updateAppointmentStatusIfNeeded(Appointment appointment, UpdateAppointmentRequest request) {
        AppointmentStatus newStatus = request.getStatus();

        if (newStatus == null || appointment.getStatus() == newStatus) {
            return;
        }

        // Kiểm tra luồng chuyển trạng thái hợp lệ của lịch hẹn để cố vấn không thể đưa lịch về trạng thái sai quy trình.
        validateStatusTransition(appointment.getStatus(), newStatus);

        Instant now = Instant.now();
        appointment.setStatus(newStatus);

        // Cập nhật các trường thời gian tương ứng với từng trạng thái
        switch (newStatus) {
            case CONFIRMED -> appointment.setConfirmedAt(now);
            case COMPLETED -> appointment.setCompletedAt(now);
            case CANCELED -> {
                appointment.setCanceledAt(now);
                if (!StringUtils.hasText(appointment.getCancelReason())) {
                    throw new CustomException(400, "Vui lòng nhập lý do hủy lịch hẹn");
                }
            }
            case EXPIRED -> appointment.setExpiredAt(now);
            default -> {
            }
        }
    }

    // Kiểm tra luồng chuyển trạng thái hợp lệ của lịch hẹn để cố vấn không thể đưa lịch về trạng thái sai quy trình.
    private void validateStatusTransition(AppointmentStatus currentStatus, AppointmentStatus newStatus) {
        boolean valid = switch (currentStatus) {
            case PENDING -> newStatus == AppointmentStatus.CONFIRMED;
            case CONFIRMED -> newStatus == AppointmentStatus.CHECKED_IN
                    || newStatus == AppointmentStatus.CANCELED
                    || newStatus == AppointmentStatus.EXPIRED;
            case CHECKED_IN -> newStatus == AppointmentStatus.COMPLETED
                    || newStatus == AppointmentStatus.CANCELED;
            case EXPIRED, COMPLETED, CANCELED -> false;
        };

        if (!valid) {
            throw new CustomException(400, "Không thể chuyển trạng thái lịch hẹn từ "
                    + currentStatus + " sang " + newStatus);
        }
    }

    // Hàm phân tích trạng thái lịch hẹn từ chuỗi đầu vào, nếu không hợp lệ thì ném lỗi 400 với thông điệp tùy chỉnh, nếu chuỗi rỗng hoặc null thì mặc định là PENDING
    private AppointmentStatus parseAppointmentStatus(String status) {
        String normalizedStatus = normalize(status);

        if (!StringUtils.hasText(normalizedStatus)) {
            return AppointmentStatus.PENDING;
        }

        try {
            return AppointmentStatus.valueOf(normalizedStatus.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new CustomException(400, "Trạng thái lịch hẹn không hợp lệ");
        }
    }

    // Hàm chuyển đổi Appointment entity sang AppointmentManagementDetailResponse bao gồm cả thông tin khách hàng (loại khách, tên đầy đủ, email, số điện thoại) được xây dựng từ GuestInformation nếu là guest hoặc UserContact nếu là user đã đăng nhập, đồng thời định dạng lại ngày hẹn và giờ theo múi giờ của doanh nghiệp
    private AppointmentManagementDetailResponse toManagementDetailResponse(Appointment appointment) {
        CustomerInformation customerInformation = buildCustomerInformation(appointment);

        ZonedDateTime start = appointment.getScheduledStartAt()
                .atZone(bookingProperties.getBusinessZone());

        ZonedDateTime end = appointment.getScheduledEndAt()
                .atZone(bookingProperties.getBusinessZone());

        return new AppointmentManagementDetailResponse(
                appointment.getId(),
                appointment.getUserId(),
                customerInformation.customerType(),
                customerInformation.fullName(),
                customerInformation.email(),
                customerInformation.phone(),
                appointment.getType(),
                appointment.getStatus(),
                start.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime(),
                appointment.getDealershipId(),
                appointment.getCarVersionId(),
                appointment.getVinId(),
                appointment.getNotes(),
                appointment.getConfirmedAt(),
                appointment.getCompletedAt(),
                appointment.getCanceledAt(),
                appointment.getExpiredAt(),
                appointment.getCancelReason(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }

    // Hàm xây dựng thông tin khách hàng (loại khách, tên đầy đủ, email, số điện thoại) được xây dựng từ GuestInformation nếu là guest hoặc UserContact nếu là user đã đăng nhập, nếu không tìm thấy thông tin khách hàng nào thì trả về loại khách UNKNOWN với các trường còn lại là null
    private CustomerInformation buildCustomerInformation(Appointment appointment) {
        GuestInformation guest = appointment.getGuestInformation();

        if (guest != null) {
            return new CustomerInformation("GUEST", guest.getFullName(), guest.getEmail(), guest.getPhone());
        }

        if (appointment.getUserId() == null) {
            return new CustomerInformation("UNKNOWN", null, null, null);
        }

        return userProfileRepository.findContactByUserId(appointment.getUserId())
                .map(userContact -> new CustomerInformation(
                        "USER",
                        userContact.getFullname(),
                        userContact.getEmail(),
                        userContact.getPhone()
                ))
                .orElseGet(() -> new CustomerInformation("USER", null, null, null));
    }

    // Hàm kiểm tra các chính sách đặt lịch trước khi tạo lịch hẹn mới, bao gồm:
    // - Kiểm tra user/guest có đang trong thời gian chờ giữa các lần đặt lịch hay không (cooldown)
    // - Kiểm tra user/guest đã đặt quá số lượng lịch hẹn cho phép
    private void validateCreatePolicy(UUID userId, GuestInformation guest, String clientIp) {

        // Nếu là user đã đăng nhập thì kiểm tra dựa trên userId
        if (userId != null) {
            // Kiểm tra giới hạn thời gian giữa các lần đặt lịch của user (cooldown)
            checkLimit("appointment:cooldown:user:" + userId, "Vui lòng chờ 60 giây trước khi đặt lịch tiếp theo");

            // Kiểm tra giới hạn số lượng lịch hẹn trong ngày của user
            checkDailyLimit("appointment:daily:user:" + userId + ":" + LocalDate.now(bookingProperties.getBusinessZone()), bookingProperties.getUserDailyLimit());

            return;
        }

        // Nếu là guest thì kiểm tra dựa trên email, số điện thoại và IP
        String email = guest.getEmail();
        String phone = guest.getPhone();
        String guestKey = buildGuestKey(email, phone, clientIp);

        // Kiểm tra giới hạn thời gian giữa các lần đặt lịch của guest (cooldown)
        checkLimit("appointment:cooldown:guest:" + guestKey, "Vui lòng chờ 120 giây trước khi đặt lịch tiếp theo");

        // Kiểm tra giới hạn số lượng lịch hẹn trong ngày của guest
        checkDailyLimit("appointment:daily:guest:" + guestKey + ":" + LocalDate.now(bookingProperties.getBusinessZone()), bookingProperties.getGuestDailyLimit());

    }

    // Kiểm tra số VIN có tồn tại trong hệ thống qua API của car-service hay không, nếu không tồn tại thì không cho phép tạo lịch hẹn dịch vụ (tạm thời bỏ qua)
//    private void validateVinExists(String vinId) {
//        String encodedVin = URLEncoder.encode(vinId, StandardCharsets.UTF_8);
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("http://" + carServiceProperties.getHost() + ":" + carServiceProperties.getPort() + "/cars/" + encodedVin))
//                .timeout(Duration.ofSeconds(3))
//                .GET()
//                .build();
//
//        try {
//            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
//
//            if (response.statusCode() == 404) {
//                throw new CustomException(404, "Số VIN không tồn tại trong hệ thống");
//            }
//
//            if (response.statusCode() < 200 || response.statusCode() >= 300) {
//                throw new CustomException(502, "Không thể kiểm tra số VIN lúc này");
//            }
//        } catch (CustomException exception) {
//            throw exception;
//        } catch (Exception exception) {
//            throw new CustomException(502, "Không thể kiểm tra số VIN lúc này");
//        }
//    }

    // Hàm đánh dấu đã sử dụng chính sách đặt lịch sau khi tạo lịch hẹn thành công, bao gồm:
    // - Đánh dấu thời gian chờ giữa các lần đặt lịch (cooldown)
    // - Đánh dấu đã đặt một lịch hẹn trong ngày (để áp dụng giới hạn số lượng lịch hẹn trong ngày)
    private void markCreatePolicy(UUID userId, GuestInformation guest, String clientIp) {

        // Nếu là user đã đăng nhập thì đánh dấu dựa trên userId
        if (userId != null) {
            markLimit("appointment:cooldown:user:" + userId, bookingProperties.getUserCooldown());
            markDaily("appointment:daily:user:" + userId + ":" + LocalDate.now(bookingProperties.getBusinessZone()));
            return;
        }

        // Nếu là guest thì đánh dấu dựa trên email, số điện thoại và IP
        String guestKey = buildGuestKey(guest.getEmail(), guest.getPhone(), clientIp);
        markLimit("appointment:cooldown:guest:" + guestKey, bookingProperties.getGuestCooldown());
        markDaily("appointment:daily:guest:" + guestKey + ":" + LocalDate.now(bookingProperties.getBusinessZone()));
    }

    // Hàm kiểm tra giới hạn thời gian giữa các lần đặt lịch (cooldown) nếu vi phạm thì ném lỗi 429
    private void checkLimit(String key, String message) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            throw new CustomException(429, message);
        }
    }

    // Hàm kiểm tra giới hạn số lượng lịch hẹn trong ngày, nếu vi phạm thì ném lỗi 429
    private void checkDailyLimit(String key, int limit) {
        Object value = redisTemplate.opsForValue().get(key);
        int count = value == null ? 0 : Integer.parseInt(value.toString());

        if (count >= limit) {
            throw new CustomException(429, "Bạn đã đặt quá số lượng lịch hẹn cho phép trong ngày");
        }
    }

    // Hàm đánh dấu thời gian chờ giữa các lần đặt lịch (cooldown) bằng cách lưu một khóa trong Redis với thời gian tồn tại (TTL) tương ứng
    // khi có khóa này thì có nghĩa là đang trong thời gian chờ và không cho phép đặt lịch mới
    private void markLimit(String key, Duration ttl) {
        redisTemplate.opsForValue().set(key, "1", ttl);
    }

    // Hàm đánh dấu đã đặt một lịch hẹn trong ngày bằng cách tăng giá trị đếm lên 1, nếu là lần đầu tiên thì thiết lập thời gian tồn tại (TTL) đến hết ngày
    private void markDaily(String key) {
        Long count = redisTemplate.opsForValue().increment(key);

        // Nếu count là 1 nghĩa là lần đầu tiên đặt lịch hẹn trong ngày, ta sẽ thiết lập TTL để khóa này tự động hết hạn vào cuối ngày (tức là sau khoảng thời gian còn lại của ngày hôm đó)
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
    }

    // Hàm tạo thông tin khách vãng lai nếu userId là null, nếu userId không null thì trả về null để bỏ qua phần này
    private GuestInformation createGuestInformationIfNeeded(UUID userId, String fullName, String email, String phone) {
        if (userId != null) {
            return null;
        }

        if (!StringUtils.hasText(fullName) || !StringUtils.hasText(email) || !StringUtils.hasText(phone)) {
            throw new CustomException(400, "Khách vãng lai cần nhập họ tên, email và số điện thoại");
        }

        GuestInformation guestInformation = GuestInformation.builder()
                .fullName(fullName.trim())
                .email(email.trim().toLowerCase())
                .phone(phone.trim())
                .build();

        return guestInformationRepository.save(guestInformation);
    }

    // Hàm phân tích và kiểm tra định dạng UUID, nếu không hợp lệ thì ném lỗi 400 với thông điệp tùy chỉnh
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

    // Hàm chuẩn hóa số VIN: loại bỏ khoảng trắng, chuyển thành chữ hoa, kiểm tra độ dài phải đúng 17 ký tự, nếu không hợp lệ thì ném lỗi 400 với thông điệp tùy chỉnh
    private String normalizeVin(String vinId) {
        String normalizedVin = normalizeRequired(vinId, "Số VIN không được để trống").toUpperCase();

        if (normalizedVin.length() != 17) {
            throw new CustomException(400, "Số VIN phải gồm 17 ký tự");
        }

        return normalizedVin;
    }

    // Hàm chuẩn hóa chuỗi bắt buộc: loại bỏ khoảng trắng, nếu sau khi chuẩn hóa mà chuỗi rỗng thì ném lỗi 400 với thông điệp tùy chỉnh
    private String normalizeRequired(String value, String message) {
        String normalizedValue = normalize(value);

        if (!StringUtils.hasText(normalizedValue)) {
            throw new CustomException(400, message);
        }

        return normalizedValue;
    }

    // Hàm chuẩn hóa chuỗi không bắt buộc: loại bỏ khoảng trắng, nếu sau khi chuẩn hóa mà chuỗi rỗng thì trả về null
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // Hàm xây dựng khóa duy nhất cho guest dựa trên email, số điện thoại và IP để áp dụng chính sách đặt lịch cho guest mà không cần userId, vì guest không có userId để phân biệt
    private String buildGuestKey(String email, String phone, String clientIp) {
        return email.toLowerCase() + ":" + phone + ":" + clientIp;
    }

    // Lớp record để đại diện cho thông tin khách hàng (loại khách, tên đầy đủ, email, số điện thoại) được xây dựng từ GuestInformation nếu là guest hoặc UserContact nếu là user đã đăng nhập, được sử dụng trong phần chi tiết lịch hẹn cho quản lý/admin
    private record CustomerInformation(String customerType, String fullName, String email, String phone) {
    }
}
