package com.tayota.userservice.service;

import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.config.AppointmentBookingProperties;
import com.tayota.userservice.config.CarServiceProperties;
import com.tayota.userservice.dto.Request.CreateServiceAppointmentRequest;
import com.tayota.userservice.dto.Request.CreateTestDriveAppointmentRequest;
import com.tayota.userservice.dto.Response.AppointmentCreatedResponse;
import com.tayota.userservice.dto.Response.AppointmentResponse;
import com.tayota.userservice.entity.Appointment;
import com.tayota.userservice.entity.GuestInformation;
import com.tayota.userservice.enums.AppointmentStatus;
import com.tayota.userservice.enums.AppointmentType;
import com.tayota.userservice.mapper.AppointmentCreatedMapper;
import com.tayota.userservice.mapper.AppointmentMapper;
import com.tayota.userservice.repository.AppointmentRepository;
import com.tayota.userservice.repository.GuestInformationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// Dịch vụ quản lý lịch hẹn lái thử và dịch vụ, bao gồm cả việc tạo lịch hẹn mới và lấy danh sách lịch hẹn của user
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final GuestInformationRepository guestInformationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppointmentBookingProperties bookingProperties;
    private final CarServiceProperties carServiceProperties;

    private final AppointmentMapper appointmentMapper;
    private final AppointmentCreatedMapper appointmentCreatedMapper;

    // Dịch vụ tạo lịch hẹn lái thử cho cả user đã đăng nhập và guest chưa đăng nhập, phân biệt bằng userId có null hay không
    @Transactional
    public AppointmentCreatedResponse createTestDriveAppointment(CreateTestDriveAppointmentRequest request, UUID userId, String clientIp) {
        // Kiểm tra đại lý có tồn tại không
        UUID dealershipId = parseUuid(request.getDealershipId(), "Đại lý không hợp lệ");

        // Kiểm tra phiên bản xe có tồn tại không
        UUID carVersionId = parseUuid(request.getCarVersionId(), "Phiên bản xe không hợp lệ");

        // Xây dựng khoảng thời gian hẹn hợp lệ
        AppointmentTimeRange timeRange = buildAppointmentTimeRange(request.getAppointmentDate(), request.getStartTime());

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
        AppointmentTimeRange timeRange = buildAppointmentTimeRange(request.getAppointmentDate(), request.getStartTime());

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
                .build();

        // Lưu lịch hẹn vào database
        Appointment saved = appointmentRepository.save(appointment);

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

    // Hàm xây dựng khoảng thời gian hẹn từ ngày hẹn và giờ bắt đầu, đồng thời kiểm tra tính hợp lệ của chúng
    private AppointmentTimeRange buildAppointmentTimeRange(LocalDate appointmentDate, LocalTime startTime) {

        // Kiểm tra ngày hẹn và giờ bắt đầu có được cung cấp hay không
        if (appointmentDate == null || startTime == null) {
            throw new CustomException(400, "Vui lòng chọn ngày và khung giờ hẹn");
        }

        // Kiểm tra ngày hẹn có nằm trong khoảng cho phép hay không (từ ngày mai đến tối đa 4 tháng tiếp theo)
        LocalDate minDate = LocalDate.now(bookingProperties.getBusinessZone()).plusDays(1);
        LocalDate maxDate = minDate.plusMonths(4);

        // Nếu ngày hẹn nằm ngoài khoảng này thì báo lỗi
        if (appointmentDate.isBefore(minDate) || appointmentDate.isAfter(maxDate)) {
            throw new CustomException(400, "Chỉ được đặt lịch từ ngày mai đến tối đa 4 tháng tiếp theo");
        }

        // Kiểm tra giờ bắt đầu có nằm trong khung giờ cho phép hay không (ví dụ: 8:00, 8:30, 9:00, ..., 16:30)
        if (!bookingProperties.getAllowedStartTimes().contains(startTime)) {
            throw new CustomException(400, "Khung giờ không hợp lệ");
        }

        // Xây dựng khoảng thời gian hẹn dựa trên ngày hẹn, giờ bắt đầu và độ dài khung giờ (slot duration) được cấu hình trong properties
        Instant startAt = appointmentDate.atTime(startTime)
                .atZone(bookingProperties.getBusinessZone())
                .toInstant();

        // Kết thúc khung giờ sẽ là giờ bắt đầu cộng với độ dài khung giờ (ví dụ: 30 phút)
        Instant endAt = appointmentDate.atTime(startTime.plus(bookingProperties.getSlotDuration()))
                .atZone(bookingProperties.getBusinessZone())
                .toInstant();

        return new AppointmentTimeRange(startAt, endAt);
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

    // Lớp record để đại diện cho khoảng thời gian hẹn với thời điểm bắt đầu và kết thúc, được xây dựng từ ngày hẹn và giờ bắt đầu, đồng thời đã được kiểm tra tính hợp lệ
    private record AppointmentTimeRange(Instant startAt, Instant endAt) {
    }
}
