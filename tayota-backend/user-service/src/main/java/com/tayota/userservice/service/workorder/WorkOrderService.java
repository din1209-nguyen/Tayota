package com.tayota.userservice.service.workorder;

import com.tayota.commoncore.exception.CustomException;
import com.tayota.commoncore.util.SecurityContextUtil;
import com.tayota.userservice.dto.Request.workorder.CreateServiceItemRequest;
import com.tayota.userservice.dto.Response.workorder.ServiceTicketDetailResponse;
import com.tayota.userservice.dto.Response.workorder.ServiceTicketSummaryResponse;
import com.tayota.userservice.entity.appointment.Appointment;
import com.tayota.userservice.entity.workorder.ServiceItem;
import com.tayota.userservice.entity.workorder.ServiceTicket;
import com.tayota.userservice.enums.appointment.AppointmentStatus;
import com.tayota.userservice.enums.workorder.BillingType;
import com.tayota.userservice.enums.workorder.ServiceTicketStatus;
import com.tayota.userservice.mapper.workorder.WorkOrderMapper;
import com.tayota.userservice.repository.appointment.AppointmentRepository;
import com.tayota.userservice.repository.workorder.ServiceItemRepository;
import com.tayota.userservice.repository.workorder.ServiceTicketRepository;
import com.tayota.userservice.service.appointment.AppointmentNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentNotificationService appointmentNotificationService;
    private final WorkOrderMapper workOrderMapper;

    // Kỹ thuật viên xem danh sách phiếu dịch vụ được giao cho mình.
    @Transactional(readOnly = true)
    public List<ServiceTicketSummaryResponse> getMyServiceTickets() {
        UUID mechanicId = getCurrentUserId();

        return serviceTicketRepository.findByMechanicIdOrderByCreatedAtDesc(mechanicId)
                .stream()
                .map(workOrderMapper::toSummaryResponse)
                .toList();
    }

    // Kỹ thuật viên tiếp nhận xe, chuyển phiếu dịch vụ từ CONFIRMED sang RECEIVING.
    @Transactional
    public ServiceTicketSummaryResponse receiveServiceTicket(UUID serviceTicketId) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);

        if (serviceTicket.getStatus() != ServiceTicketStatus.CONFIRMED) {
            throw new CustomException(400, "Chỉ có thể tiếp nhận phiếu dịch vụ đã được xác nhận");
        }

        serviceTicket.setStatus(ServiceTicketStatus.RECEIVING);
        serviceTicket.setReceivingAt(Instant.now());

        return workOrderMapper.toSummaryResponse(serviceTicketRepository.save(serviceTicket));
    }

    // Kỹ thuật viên bắt đầu sửa, chuyển phiếu dịch vụ từ RECEIVING sang IN_PROGRESS.
    @Transactional
    public ServiceTicketSummaryResponse startServiceTicket(UUID serviceTicketId) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);

        if (serviceTicket.getStatus() != ServiceTicketStatus.RECEIVING) {
            throw new CustomException(400, "Chỉ có thể bắt đầu sửa phiếu dịch vụ đã được tiếp nhận");
        }

        serviceTicket.setStatus(ServiceTicketStatus.IN_PROGRESS);
        serviceTicket.setProcessingAt(Instant.now());

        return workOrderMapper.toSummaryResponse(serviceTicketRepository.save(serviceTicket));
    }

    // Trong lúc sửa, kỹ thuật viên thêm hạng mục dịch vụ thủ công và hệ thống tự tính lại tổng tiền.
    @Transactional
    public ServiceTicketDetailResponse addServiceItem(UUID serviceTicketId, CreateServiceItemRequest request) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);

        if (serviceTicket.getStatus() != ServiceTicketStatus.IN_PROGRESS) {
            throw new CustomException(400, "Chỉ có thể thêm hạng mục khi phiếu dịch vụ đang được sửa");
        }

        ServiceItem serviceItem = ServiceItem.builder()
                .serviceTicket(serviceTicket)
                .itemType(request.getItemType())
                .accessoryId(request.getAccessoryId())
                .itemName(normalizeRequired(request.getItemName(), "Tên hạng mục không được để trống"))
                .quantity(request.getQuantity())
                .unitPrice(resolveUnitPrice(request))
                .billingType(request.getBillingType())
                .finalPrice(calculateFinalPrice(request))
                .note(normalize(request.getNote()))
                .build();

        serviceItemRepository.save(serviceItem);
        recalculateTotalAmount(serviceTicket);

        return getServiceTicketDetail(serviceTicket.getId());
    }

    // Kỹ thuật viên xem chi tiết phiếu dịch vụ, bao gồm danh sách hạng mục dịch vụ đã thêm.
    @Transactional(readOnly = true)
    public ServiceTicketDetailResponse getServiceTicketDetail(UUID serviceTicketId) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);
        var items = serviceItemRepository.findByServiceTicketIdOrderByCreatedAtAsc(serviceTicketId)
                .stream()
                .map(workOrderMapper::toServiceItemResponse)
                .toList();

        return new ServiceTicketDetailResponse(workOrderMapper.toInfoResponse(serviceTicket), items);
    }

    // Kỹ thuật viên hoàn thành sửa, chuyển phiếu dịch vụ từ IN_PROGRESS sang COMPLETED.
    @Transactional
    public ServiceTicketDetailResponse completeServiceTicket(UUID serviceTicketId) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);

        if (serviceTicket.getStatus() != ServiceTicketStatus.IN_PROGRESS) {
            throw new CustomException(400, "Chỉ có thể hoàn thành phiếu dịch vụ đang sửa");
        }

        Appointment appointment = serviceTicket.getAppointment();
        if (appointment == null) {
            throw new CustomException(400, "Phiếu dịch vụ chưa liên kết với lịch hẹn");
        }

        // Cập nhật trạng thái và thời gian hoàn thành cho cả phiếu dịch vụ và lịch hẹn, đảm bảo tính nhất quán.
        Instant now = Instant.now();
        serviceTicket.setStatus(ServiceTicketStatus.COMPLETED);
        serviceTicket.setCompletedAt(now);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(now);

        // Lưu thay đổi cho cả phiếu dịch vụ và lịch hẹn trước khi gửi thông báo, đảm bảo rằng thông tin đã được cập nhật chính xác khi FE lấy lại dữ liệu.
        serviceTicketRepository.save(serviceTicket);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Gửi thông báo cảm ơn và lời mời đánh giá sau khi lịch hẹn hoàn thành. Thông báo này sẽ được gửi đến user nếu có userId, hoặc gửi email nếu có thông tin liên hệ.
        appointmentNotificationService.notifyAppointmentCompleted(savedAppointment);

        return getServiceTicketDetail(serviceTicket.getId());
    }

    private ServiceTicket getMyServiceTicket(UUID serviceTicketId) {
        UUID mechanicId = getCurrentUserId();
        ServiceTicket serviceTicket = serviceTicketRepository.findById(serviceTicketId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiếu dịch vụ"));

        if (!mechanicId.equals(serviceTicket.getMechanicId())) {
            throw new CustomException(403, "Bạn không có quyền xử lý phiếu dịch vụ này");
        }

        return serviceTicket;
    }

    // Phương thức tiện ích để tính lại tổng tiền của phiếu dịch vụ sau khi thêm/sửa/xóa hạng mục dịch vụ.
    private void recalculateTotalAmount(ServiceTicket serviceTicket) {
        BigDecimal totalAmount = serviceItemRepository.findByServiceTicketId(serviceTicket.getId())
                .stream()
                .map(ServiceItem::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        serviceTicket.setTotalAmount(totalAmount);
        serviceTicketRepository.save(serviceTicket);
    }

    // Phương thức tiện ích để tính giá cuối cùng của hạng mục dịch vụ dựa trên hình thức tính phí.
    private BigDecimal calculateFinalPrice(CreateServiceItemRequest request) {
        // Nếu hình thức tính phí là WARRANTY hoặc GIFT thì giá cuối cùng luôn bằng 0, bất kể đơn giá và số lượng.
        if (request.getBillingType() == BillingType.WARRANTY || request.getBillingType() == BillingType.GIFT) {
            return BigDecimal.ZERO;
        }

        return resolveUnitPrice(request).multiply(BigDecimal.valueOf(request.getQuantity()));
    }

    // Phương thức tiện ích để xác định đơn giá của hạng mục dịch vụ, chỉ áp dụng khi hình thức tính phí là NORMAL. Nếu là WARRANTY hoặc GIFT thì đơn giá luôn bằng 0.
    private BigDecimal resolveUnitPrice(CreateServiceItemRequest request) {
        if (request.getBillingType() == BillingType.WARRANTY || request.getBillingType() == BillingType.GIFT) {
            return BigDecimal.ZERO;
        }

        if (request.getUnitPrice() == null) {
            throw new CustomException(400, "Đơn giá không được để trống khi tính phí bình thường");
        }

        if (request.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(400, "Đơn giá không được âm");
        }

        return request.getUnitPrice();
    }

    // Phương thức tiện ích để lấy userId của kỹ thuật viên đang đăng nhập từ SecurityContext, đảm bảo rằng thông tin này luôn có và hợp lệ.
    private UUID getCurrentUserId() {
        return UUID.fromString(SecurityContextUtil.getCurrentUserId());
    }

    // Phương thức tiện ích để chuẩn hóa và xác thực các trường bắt buộc, đảm bảo rằng chúng không null hoặc rỗng sau khi loại bỏ khoảng trắng. Nếu không hợp lệ, ném ra CustomException với mã lỗi và thông điệp cụ thể.
    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);

        if (!StringUtils.hasText(normalized)) {
            throw new CustomException(400, message);
        }

        return normalized;
    }

    // Phương thức tiện ích để chuẩn hóa các trường không bắt buộc, loại bỏ khoảng trắng ở đầu và cuối. Nếu sau khi chuẩn hóa mà chuỗi rỗng thì trả về null.
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
