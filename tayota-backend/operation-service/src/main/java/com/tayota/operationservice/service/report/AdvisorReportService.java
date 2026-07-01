package com.tayota.operationservice.service.report;

import com.tayota.operationservice.dto.response.report.AdvisorReportCompletedTicketResponse;
import com.tayota.operationservice.dto.response.report.AdvisorReportRatingDistributionResponse;
import com.tayota.operationservice.dto.response.report.AdvisorReportResponse;
import com.tayota.operationservice.dto.response.report.AdvisorReportRevenueByDayResponse;
import com.tayota.operationservice.dto.response.report.AdvisorReportReviewResponse;
import com.tayota.operationservice.dto.response.report.AdvisorReportStatusCountResponse;
import com.tayota.operationservice.dto.response.report.AdvisorReportSummaryResponse;
import com.tayota.operationservice.entity.appointment.Appointment;
import com.tayota.operationservice.entity.review.CustomerReview;
import com.tayota.operationservice.entity.user.ServiceAdvisor;
import com.tayota.operationservice.entity.workorder.ServiceTicket;
import com.tayota.operationservice.enums.appointment.AppointmentStatus;
import com.tayota.operationservice.enums.appointment.AppointmentType;
import com.tayota.operationservice.enums.review.ReviewStatus;
import com.tayota.operationservice.enums.review.ReviewType;
import com.tayota.operationservice.enums.workorder.ServiceTicketStatus;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.repository.appointment.AppointmentRepository;
import com.tayota.operationservice.repository.review.CustomerReviewRepository;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.workorder.ServiceTicketRepository;
import com.tayota.operationservice.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvisorReportService {
    private static final int RECENT_LIMIT = 10;
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Bangkok");

    private final AppointmentRepository appointmentRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final CustomerReviewRepository customerReviewRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public AdvisorReportResponse getAdvisorReport(LocalDate from, LocalDate to, String type) {
        ReportType reportType = parseReportType(type);
        LocalDate fromDate = from == null ? LocalDate.now(REPORT_ZONE) : from;
        LocalDate toDate = to == null ? fromDate : to;
        if (toDate.isBefore(fromDate)) {
            throw new CustomException(400, "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }

        Instant fromInclusive = fromDate.atStartOfDay(REPORT_ZONE).toInstant();
        Instant toExclusive = toDate.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant();
        UUID dealershipId = getCurrentAdvisorDealershipId();
        AppointmentType appointmentType = reportType.toAppointmentType();
        ReviewType reviewType = reportType.toReviewType();

        List<Appointment> appointments = appointmentRepository.findAdvisorReportAppointments(
                dealershipId,
                fromInclusive,
                toExclusive,
                appointmentType
        );
        List<ServiceTicket> ticketsByCreatedAt = reportType.includesService()
                ? serviceTicketRepository.findAdvisorReportTicketsByCreatedAt(dealershipId, fromInclusive, toExclusive)
                : List.of();
        List<ServiceTicket> completedTickets = reportType.includesService()
                ? serviceTicketRepository.findAdvisorReportCompletedTickets(dealershipId, ServiceTicketStatus.COMPLETED, fromInclusive, toExclusive)
                : List.of();
        List<CustomerReview> reviews = customerReviewRepository.findAdvisorReportSubmittedReviews(
                dealershipId,
                ReviewStatus.SUBMITTED,
                fromInclusive,
                toExclusive,
                reviewType
        );

        return new AdvisorReportResponse(
                buildSummary(appointments, ticketsByCreatedAt, completedTickets, reviews),
                buildAppointmentStatus(appointments),
                buildTicketStatus(ticketsByCreatedAt),
                buildRevenueByDay(fromDate, toDate, completedTickets),
                buildRatingDistribution(reviews),
                buildRecentTickets(completedTickets),
                buildRecentReviews(reviews)
        );
    }

    private AdvisorReportSummaryResponse buildSummary(
            List<Appointment> appointments,
            List<ServiceTicket> ticketsByCreatedAt,
            List<ServiceTicket> completedTickets,
            List<CustomerReview> reviews
    ) {
        BigDecimal revenue = completedTickets.stream()
                .map(ServiceTicket::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double average = reviews.stream()
                .map(CustomerReview::getServiceRating)
                .filter(Objects::nonNull)
                .mapToInt(Short::intValue)
                .average()
                .orElse(0);

        return new AdvisorReportSummaryResponse(
                appointments.size(),
                appointments.stream().filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED).count(),
                appointments.stream().filter(appointment -> appointment.getStatus() == AppointmentStatus.CANCELED || appointment.getStatus() == AppointmentStatus.EXPIRED).count(),
                ticketsByCreatedAt.size(),
                revenue,
                average <= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP)
        );
    }

    private List<AdvisorReportStatusCountResponse> buildAppointmentStatus(List<Appointment> appointments) {
        Map<AppointmentStatus, Long> counts = appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getStatus, () -> new EnumMap<>(AppointmentStatus.class), Collectors.counting()));
        List<AdvisorReportStatusCountResponse> response = new ArrayList<>();
        for (AppointmentStatus status : AppointmentStatus.values()) {
            response.add(new AdvisorReportStatusCountResponse(status.name(), counts.getOrDefault(status, 0L)));
        }
        return response;
    }

    private List<AdvisorReportStatusCountResponse> buildTicketStatus(List<ServiceTicket> tickets) {
        Map<ServiceTicketStatus, Long> counts = tickets.stream()
                .collect(Collectors.groupingBy(ServiceTicket::getStatus, () -> new EnumMap<>(ServiceTicketStatus.class), Collectors.counting()));
        List<AdvisorReportStatusCountResponse> response = new ArrayList<>();
        for (ServiceTicketStatus status : ServiceTicketStatus.values()) {
            response.add(new AdvisorReportStatusCountResponse(status.name(), counts.getOrDefault(status, 0L)));
        }
        return response;
    }

    private List<AdvisorReportRevenueByDayResponse> buildRevenueByDay(LocalDate fromDate, LocalDate toDate, List<ServiceTicket> completedTickets) {
        Map<LocalDate, BigDecimal> revenueByDate = completedTickets.stream()
                .filter(ticket -> ticket.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        ticket -> LocalDate.ofInstant(ticket.getCompletedAt(), REPORT_ZONE),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                ticket -> ticket.getTotalAmount() == null ? BigDecimal.ZERO : ticket.getTotalAmount(),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
        List<AdvisorReportRevenueByDayResponse> response = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            response.add(new AdvisorReportRevenueByDayResponse(date, revenueByDate.getOrDefault(date, BigDecimal.ZERO)));
        }
        return response;
    }

    private List<AdvisorReportRatingDistributionResponse> buildRatingDistribution(List<CustomerReview> reviews) {
        Map<Integer, Long> counts = reviews.stream()
                .map(CustomerReview::getServiceRating)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Short::intValue, Collectors.counting()));
        List<AdvisorReportRatingDistributionResponse> response = new ArrayList<>();
        for (int rating = 1; rating <= 5; rating += 1) {
            response.add(new AdvisorReportRatingDistributionResponse(rating, counts.getOrDefault(rating, 0L)));
        }
        return response;
    }

    private List<AdvisorReportCompletedTicketResponse> buildRecentTickets(List<ServiceTicket> completedTickets) {
        Map<UUID, String> mechanicNames = loadNames(completedTickets.stream()
                .map(ServiceTicket::getMechanicId)
                .filter(Objects::nonNull)
                .toList());

        return completedTickets.stream()
                .limit(RECENT_LIMIT)
                .map(ticket -> new AdvisorReportCompletedTicketResponse(
                        ticket.getId(),
                        resolveTicketCustomerName(ticket),
                        ticket.getVinId(),
                        ticket.getMechanicId(),
                        mechanicNames.get(ticket.getMechanicId()),
                        ticket.getTotalAmount() == null ? BigDecimal.ZERO : ticket.getTotalAmount(),
                        ticket.getCompletedAt()
                ))
                .toList();
    }

    private List<AdvisorReportReviewResponse> buildRecentReviews(List<CustomerReview> reviews) {
        Map<UUID, String> userNames = loadNames(reviews.stream()
                .map(CustomerReview::getUserId)
                .filter(Objects::nonNull)
                .toList());

        return reviews.stream()
                .limit(RECENT_LIMIT)
                .map(review -> new AdvisorReportReviewResponse(
                        review.getId(),
                        review.getReviewType().name(),
                        review.getAppointment() == null ? null : review.getAppointment().getId(),
                        review.getServiceTicket() == null ? null : review.getServiceTicket().getId(),
                        resolveReviewCustomerName(review, userNames),
                        review.getServiceRating() == null ? null : review.getServiceRating().intValue(),
                        review.getServiceComment(),
                        review.getSubmittedAt()
                ))
                .toList();
    }

    private Map<UUID, String> loadNames(List<UUID> ids) {
        return ids.stream()
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::resolveUserName, (left, right) -> left));
    }

    private String resolveUserName(UUID userId) {
        return userProfileRepository.findById(userId)
                .map(profile -> StringUtils.hasText(profile.getFullname()) ? profile.getFullname() : profile.getId().toString())
                .orElse(userId.toString());
    }

    private String resolveTicketCustomerName(ServiceTicket ticket) {
        if (ticket.getGuestInformation() != null && StringUtils.hasText(ticket.getGuestInformation().getFullName())) {
            return ticket.getGuestInformation().getFullName();
        }
        if (ticket.getUserId() != null) {
            return resolveUserName(ticket.getUserId());
        }
        return "Khách hàng";
    }

    private String resolveReviewCustomerName(CustomerReview review, Map<UUID, String> userNames) {
        if (StringUtils.hasText(review.getGuestFullName())) {
            return review.getGuestFullName();
        }
        if (review.getUserId() != null) {
            return userNames.getOrDefault(review.getUserId(), review.getUserId().toString());
        }
        return "Khách hàng";
    }

    private UUID getCurrentAdvisorDealershipId() {
        UUID currentUserId = UUID.fromString(SecurityContextUtil.getCurrentUserId());
        ServiceAdvisor advisor = serviceAdvisorRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(403, "Tài khoản cố vấn dịch vụ chưa được gán đại lý"));

        return advisor.getDealershipId();
    }

    private ReportType parseReportType(String type) {
        if (!StringUtils.hasText(type) || "ALL".equalsIgnoreCase(type)) {
            return ReportType.ALL;
        }
        if ("SERVICE".equalsIgnoreCase(type)) {
            return ReportType.SERVICE;
        }
        if ("TEST_DRIVE".equalsIgnoreCase(type)) {
            return ReportType.TEST_DRIVE;
        }
        throw new CustomException(400, "Loại báo cáo không hợp lệ");
    }

    private enum ReportType {
        ALL,
        TEST_DRIVE,
        SERVICE;

        private AppointmentType toAppointmentType() {
            return switch (this) {
                case TEST_DRIVE -> AppointmentType.TEST_DRIVE;
                case SERVICE -> AppointmentType.SERVICE;
                case ALL -> null;
            };
        }

        private ReviewType toReviewType() {
            return switch (this) {
                case TEST_DRIVE -> ReviewType.TEST_DRIVE;
                case SERVICE -> ReviewType.SERVICE;
                case ALL -> null;
            };
        }

        private boolean includesService() {
            return this != TEST_DRIVE;
        }
    }
}
