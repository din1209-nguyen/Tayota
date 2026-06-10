package com.tayota.operationservice.service.workorder;

import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.util.SecurityContextUtil;
import com.tayota.operationservice.dto.request.workorder.AssignMechanicRequest;
import com.tayota.operationservice.dto.request.workorder.CancelServiceTicketRequest;
import com.tayota.operationservice.dto.request.workorder.CreateServiceItemRequest;
import com.tayota.operationservice.dto.request.workorder.CreateWalkInServiceTicketRequest;
import com.tayota.operationservice.dto.request.workorder.RejectServiceTicketRequest;
import com.tayota.operationservice.dto.response.car.AccessoryResponseDTO;
import com.tayota.operationservice.dto.response.workorder.ServiceInvoiceResponse;
import com.tayota.operationservice.dto.response.workorder.ServiceTicketDetailResponse;
import com.tayota.operationservice.dto.response.workorder.ServiceTicketSummaryResponse;
import com.tayota.operationservice.entity.car.Car;
import com.tayota.operationservice.entity.car.Dealership;
import com.tayota.operationservice.entity.appointment.GuestInformation;
import com.tayota.operationservice.entity.appointment.Appointment;
import com.tayota.operationservice.entity.user.ServiceAdvisor;
import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.entity.workorder.ServiceItem;
import com.tayota.operationservice.entity.workorder.Mechanic;
import com.tayota.operationservice.entity.workorder.ServiceTicket;
import com.tayota.operationservice.enums.appointment.AppointmentStatus;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.enums.workorder.BillingType;
import com.tayota.operationservice.enums.workorder.ServiceItemType;
import com.tayota.operationservice.enums.workorder.ServiceTicketStatus;
import com.tayota.operationservice.mapper.workorder.WorkOrderMapper;
import com.tayota.operationservice.repository.appointment.GuestInformationRepository;
import com.tayota.operationservice.repository.appointment.AppointmentRepository;
import com.tayota.operationservice.repository.car.CarAccessoryRepository;
import com.tayota.operationservice.repository.car.CarRepository;
import com.tayota.operationservice.repository.car.DealershipRepository;
import com.tayota.operationservice.repository.review.CustomerReviewRepository;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.user.UserRepository;
import com.tayota.operationservice.repository.workorder.MechanicRepository;
import com.tayota.operationservice.repository.workorder.ServiceItemRepository;
import com.tayota.operationservice.repository.workorder.ServiceTicketRepository;
import com.tayota.operationservice.service.appointment.AppointmentNotificationService;
import com.tayota.operationservice.service.review.CustomerReviewService;
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
    private final GuestInformationRepository guestInformationRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final MechanicRepository mechanicRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CarRepository carRepository;
    private final CarAccessoryRepository carAccessoryRepository;
    private final DealershipRepository dealershipRepository;
    private final CustomerReviewRepository customerReviewRepository;
    private final AppointmentNotificationService appointmentNotificationService;
    private final CustomerReviewService customerReviewService;
    private final WorkOrderMapper workOrderMapper;

    // Kỹ thuật viên xem danh sách phiếu dịch vụ được giao cho mình.
    @Transactional(readOnly = true)
    public List<ServiceTicketSummaryResponse> getMyServiceTickets() {
        UUID mechanicId = getCurrentUserId();

        return serviceTicketRepository.findByMechanicIdOrderByCreatedAtDesc(mechanicId)
                .stream()
                .map(ticket -> {
                    CustomerInformation customer = buildCustomerInformation(ticket);
                    return toSummaryResponse(ticket, customer);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceTicketSummaryResponse> getUserServiceTickets() {
        UUID userId = getCurrentUserId();

        return serviceTicketRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ticket -> {
                    CustomerInformation customer = buildCustomerInformation(ticket);
                    return toSummaryResponse(ticket, customer);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceTicketDetailResponse getUserServiceTicketDetail(UUID serviceTicketId) {
        return toDetailResponse(getUserServiceTicket(serviceTicketId));
    }

    @Transactional(readOnly = true)
    public List<ServiceTicketSummaryResponse> getAdvisorServiceTickets(String status) {
        UUID dealershipId = getCurrentAdvisorDealershipId();
        List<ServiceTicket> tickets;

        if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status)) {
            tickets = serviceTicketRepository.findByDealershipIdOrderByCreatedAtDesc(dealershipId);
        }
        else {
            tickets = serviceTicketRepository.findByDealershipIdAndStatusOrderByCreatedAtDesc(
                    dealershipId,
                    parseServiceTicketStatus(status)
            );
        }

        return tickets.stream()
                .map(ticket -> {
                    CustomerInformation customer = buildCustomerInformation(ticket);
                    return toSummaryResponse(ticket, customer);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceTicketDetailResponse getAdvisorServiceTicketDetail(UUID serviceTicketId) {
        ServiceTicket serviceTicket = serviceTicketRepository.findById(serviceTicketId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiếu dịch vụ"));

        if (!getCurrentAdvisorDealershipId().equals(serviceTicket.getDealershipId())) {
            throw new CustomException(403, "Bạn không có quyền xem phiếu dịch vụ này");
        }

        return toDetailResponse(serviceTicket);
    }

    @Transactional
    public ServiceTicketSummaryResponse createWalkInServiceTicket(CreateWalkInServiceTicketRequest request) {
        UUID dealershipId = getCurrentAdvisorDealershipId();
        String vinId = normalizeVin(request.getVinId());
        if (!carRepository.existsById(vinId)) {
            throw new CustomException(404, "Không tìm thấy xe theo VIN");
        }

        Mechanic mechanic = mechanicRepository.findById(request.getMechanicId())
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy kỹ thuật viên"));
        if (!dealershipId.equals(mechanic.getDealershipId()) || Boolean.FALSE.equals(mechanic.getActive())) {
            throw new CustomException(400, "Kỹ thuật viên không thuộc đại lý hoặc không hoạt động");
        }

        User user = null;
        GuestInformation guestInformation = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new CustomException(404, "Không tìm thấy khách hàng"));
            if (user.getRole() != RoleType.USER || user.getStatus() != StatusType.ACTIVE) {
                throw new CustomException(400, "Chỉ có thể tạo dịch vụ cho tài khoản khách hàng đang hoạt động");
            }
        }
        else {
            guestInformation = createGuestInformation(request);
        }

        ServiceTicket serviceTicket = ServiceTicket.builder()
                .userId(user == null ? null : user.getId())
                .guestInformation(guestInformation)
                .vinId(vinId)
                .mechanicId(mechanic.getId())
                .dealershipId(dealershipId)
                .appointment(null)
                .mileageAtService(request.getMileageAtService())
                .status(ServiceTicketStatus.CONFIRMED)
                .totalAmount(BigDecimal.ZERO)
                .vehicleCondition(normalizeRequired(request.getVehicleCondition(), "Vui lòng nhập tình trạng xe khi tiếp nhận"))
                .notes(normalize(request.getNotes()))
                .receivingAt(Instant.now())
                .build();

        ServiceTicket saved = serviceTicketRepository.save(serviceTicket);
        CustomerInformation customer = buildCustomerInformation(saved);

        return toSummaryResponse(saved, customer);
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

        validateTicketCanEditItems(serviceTicket);
        validateServiceItemRequest(serviceTicket, request);

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

        return toDetailResponse(serviceTicket);
    }

    // Kỹ thuật viên hoàn thành sửa, chuyển phiếu dịch vụ từ IN_PROGRESS sang COMPLETED.
    @Transactional
    public ServiceTicketDetailResponse completeServiceTicket(UUID serviceTicketId) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);

        if (serviceTicket.getStatus() != ServiceTicketStatus.IN_PROGRESS) {
            throw new CustomException(400, "Chỉ có thể hoàn thành phiếu dịch vụ đang sửa");
        }

        // Cập nhật trạng thái và thời gian hoàn thành cho cả phiếu dịch vụ và lịch hẹn, đảm bảo tính nhất quán.
        Instant now = Instant.now();
        serviceTicket.setStatus(ServiceTicketStatus.COMPLETED);
        serviceTicket.setCompletedAt(now);

        serviceTicketRepository.save(serviceTicket);
        Appointment appointment = serviceTicket.getAppointment();

        if (appointment != null) {
            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointment.setCompletedAt(now);
            Appointment savedAppointment = appointmentRepository.save(appointment);
            appointmentNotificationService.notifyAppointmentCompleted(savedAppointment);
        }
        else {
            customerReviewService.createPendingReviewForServiceTicket(serviceTicket);
        }

        return getServiceTicketDetail(serviceTicket.getId());
    }

    @Transactional
    public ServiceTicketSummaryResponse rejectServiceTicket(UUID serviceTicketId, RejectServiceTicketRequest request) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);

        if (serviceTicket.getStatus() != ServiceTicketStatus.CONFIRMED) {
            throw new CustomException(400, "Chỉ có thể từ chối phiếu dịch vụ đang chờ tiếp nhận");
        }

        String reason = normalizeRequired(request.getReason(), "Vui lòng nhập lý do từ chối");
        serviceTicket.setStatus(ServiceTicketStatus.NEEDS_REASSIGNMENT);
        serviceTicket.setMechanicId(null);
        serviceTicket.setCancelReason(reason);
        serviceTicket.setNotes("Kỹ thuật viên từ chối: " + reason);

        CustomerInformation customer = buildCustomerInformation(serviceTicket);
        return toSummaryResponse(serviceTicketRepository.save(serviceTicket), customer);
    }

    @Transactional
    public ServiceTicketSummaryResponse assignMechanic(UUID serviceTicketId, AssignMechanicRequest request) {
        ServiceTicket serviceTicket = getAdvisorTicket(serviceTicketId);

        if (serviceTicket.getStatus() != ServiceTicketStatus.NEEDS_REASSIGNMENT) {
            throw new CustomException(400, "Chỉ có thể phân công lại phiếu đang chờ phân công");
        }

        Mechanic mechanic = mechanicRepository.findById(request.getMechanicId())
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy kỹ thuật viên"));

        if (!serviceTicket.getDealershipId().equals(mechanic.getDealershipId()) || Boolean.FALSE.equals(mechanic.getActive())) {
            throw new CustomException(400, "Kỹ thuật viên không thuộc đại lý hoặc không hoạt động");
        }

        serviceTicket.setMechanicId(mechanic.getId());
        serviceTicket.setStatus(ServiceTicketStatus.CONFIRMED);
        serviceTicket.setCancelReason(null);

        CustomerInformation customer = buildCustomerInformation(serviceTicket);
        return toSummaryResponse(serviceTicketRepository.save(serviceTicket), customer);
    }

    @Transactional
    public ServiceTicketSummaryResponse cancelAdvisorServiceTicket(UUID serviceTicketId, CancelServiceTicketRequest request) {
        ServiceTicket serviceTicket = getAdvisorTicket(serviceTicketId);

        if (serviceTicket.getStatus() != ServiceTicketStatus.CONFIRMED
                && serviceTicket.getStatus() != ServiceTicketStatus.NEEDS_REASSIGNMENT) {
            throw new CustomException(400, "Chỉ có thể hủy phiếu dịch vụ chưa được kỹ thuật viên tiếp nhận");
        }

        String reason = normalizeRequired(request.getReason(), "Vui lòng nhập lý do hủy phiếu dịch vụ");
        Instant now = Instant.now();

        serviceTicket.setStatus(ServiceTicketStatus.CANCELED);
        serviceTicket.setCanceledAt(now);
        serviceTicket.setCancelReason(reason);
        serviceTicket.setMechanicId(null);

        Appointment appointment = serviceTicket.getAppointment();
        if (appointment != null && appointment.getStatus() != AppointmentStatus.CANCELED) {
            appointment.setStatus(AppointmentStatus.CANCELED);
            appointment.setCanceledAt(now);
            appointment.setCancelReason(reason);
            appointment.setMechanicId(null);
            appointmentRepository.save(appointment);
        }

        CustomerInformation customer = buildCustomerInformation(serviceTicket);
        return toSummaryResponse(serviceTicketRepository.save(serviceTicket), customer);
    }

    @Transactional
    public ServiceTicketDetailResponse updateServiceItem(UUID serviceTicketId, UUID itemId, CreateServiceItemRequest request) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);
        validateTicketCanEditItems(serviceTicket);
        validateServiceItemRequest(serviceTicket, request);

        ServiceItem serviceItem = serviceItemRepository.findByIdAndServiceTicketId(itemId, serviceTicketId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy hạng mục dịch vụ"));

        serviceItem.setItemType(request.getItemType());
        serviceItem.setAccessoryId(request.getAccessoryId());
        serviceItem.setItemName(normalizeRequired(request.getItemName(), "Tên hạng mục không được để trống"));
        serviceItem.setQuantity(request.getQuantity());
        serviceItem.setUnitPrice(resolveUnitPrice(request));
        serviceItem.setBillingType(request.getBillingType());
        serviceItem.setFinalPrice(calculateFinalPrice(request));
        serviceItem.setNote(normalize(request.getNote()));

        serviceItemRepository.save(serviceItem);
        recalculateTotalAmount(serviceTicket);

        return getServiceTicketDetail(serviceTicketId);
    }

    @Transactional
    public ServiceTicketDetailResponse deleteServiceItem(UUID serviceTicketId, UUID itemId) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);
        validateTicketCanEditItems(serviceTicket);

        ServiceItem serviceItem = serviceItemRepository.findByIdAndServiceTicketId(itemId, serviceTicketId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy hạng mục dịch vụ"));

        serviceItemRepository.delete(serviceItem);
        recalculateTotalAmount(serviceTicket);

        return getServiceTicketDetail(serviceTicketId);
    }

    @Transactional(readOnly = true)
    public List<AccessoryResponseDTO> getRecommendedAccessories(UUID serviceTicketId) {
        ServiceTicket serviceTicket = getMyServiceTicket(serviceTicketId);
        Car car = findCar(serviceTicket.getVinId());

        if (car.getCarVersion() == null) {
            return List.of();
        }

        return carAccessoryRepository.findByCarVersionId(car.getCarVersion().getId())
                .stream()
                .map(carAccessory -> carAccessory.getAccessory())
                .filter(accessory -> accessory != null && accessory.isVisible())
                .map(accessory -> new AccessoryResponseDTO(
                        accessory.getId(),
                        accessory.getModel(),
                        accessory.getBrand(),
                        accessory.getPrice(),
                        accessory.getDescription(),
                        accessory.getUseContent(),
                        accessory.getReminderContent(),
                        accessory.getType(),
                        accessory.isVisible()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceInvoiceResponse getServiceInvoice(UUID serviceTicketId) {
        ServiceTicket serviceTicket = serviceTicketRepository.findById(serviceTicketId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiếu dịch vụ"));

        if (serviceTicket.getStatus() != ServiceTicketStatus.COMPLETED) {
            throw new CustomException(400, "Chỉ có thể xuất phiếu thu cho phiếu đã hoàn tất");
        }

        validateCurrentUserCanViewInvoice(serviceTicket);

        CustomerInformation customer = buildCustomerInformation(serviceTicket);
        List<com.tayota.operationservice.dto.response.workorder.ServiceItemResponse> items = serviceItemRepository
                .findByServiceTicketIdOrderByCreatedAtAsc(serviceTicketId)
                .stream()
                .map(workOrderMapper::toServiceItemResponse)
                .toList();
        Car car = carRepository.findById(serviceTicket.getVinId()).orElse(null);
        Dealership dealership = dealershipRepository.findById(serviceTicket.getDealershipId()).orElse(null);
        String mechanicName = serviceTicket.getMechanicId() == null
                ? null
                : userProfileRepository.findById(serviceTicket.getMechanicId()).map(profile -> profile.getFullname()).orElse(null);

        return new ServiceInvoiceResponse(
                "ST-" + serviceTicket.getCreatedAt().toString().substring(0, 10).replace("-", "") + "-" + serviceTicket.getId().toString().substring(0, 8).toUpperCase(),
                serviceTicket.getCompletedAt(),
                serviceTicket.getId(),
                serviceTicket.getDealershipId(),
                dealership == null ? null : dealership.getName(),
                dealership == null ? null : dealership.getAddress(),
                customer.customerType(),
                customer.fullName(),
                customer.email(),
                customer.phone(),
                serviceTicket.getVinId(),
                car == null || car.getCarVersion() == null ? null : car.getCarVersion().getId(),
                car == null || car.getCarVersion() == null ? null : car.getCarVersion().getName(),
                serviceTicket.getMechanicId(),
                mechanicName,
                serviceTicket.getMileageAtService(),
                serviceTicket.getVehicleCondition(),
                items,
                serviceTicket.getTotalAmount() == null ? BigDecimal.ZERO : serviceTicket.getTotalAmount()
        );
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

    private ServiceTicket getUserServiceTicket(UUID serviceTicketId) {
        UUID userId = getCurrentUserId();
        ServiceTicket serviceTicket = serviceTicketRepository.findById(serviceTicketId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiếu dịch vụ"));

        if (!userId.equals(serviceTicket.getUserId())) {
            throw new CustomException(403, "Bạn không có quyền xem phiếu dịch vụ này");
        }

        return serviceTicket;
    }

    private ServiceTicket getAdvisorTicket(UUID serviceTicketId) {
        ServiceTicket serviceTicket = serviceTicketRepository.findById(serviceTicketId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiếu dịch vụ"));

        if (!getCurrentAdvisorDealershipId().equals(serviceTicket.getDealershipId())) {
            throw new CustomException(403, "Bạn không có quyền xử lý phiếu dịch vụ này");
        }

        return serviceTicket;
    }

    private ServiceTicketDetailResponse toDetailResponse(ServiceTicket serviceTicket) {
        CustomerInformation customer = buildCustomerInformation(serviceTicket);
        var items = serviceItemRepository.findByServiceTicketIdOrderByCreatedAtAsc(serviceTicket.getId())
                .stream()
                .map(workOrderMapper::toServiceItemResponse)
                .toList();

        return new ServiceTicketDetailResponse(
                workOrderMapper.toInfoResponse(
                        serviceTicket,
                        customer.customerType(),
                        customer.fullName(),
                        customer.email(),
                        customer.phone()
                ),
                items
        );
    }

    private ServiceTicketSummaryResponse toSummaryResponse(ServiceTicket ticket, CustomerInformation customer) {
        return workOrderMapper.toSummaryResponse(
                ticket,
                customer.customerType(),
                customer.fullName(),
                customer.email(),
                customer.phone()
        );
    }

    private CustomerInformation buildCustomerInformation(ServiceTicket ticket) {
        GuestInformation guest = ticket.getGuestInformation();
        if (guest != null) {
            return new CustomerInformation("GUEST", guest.getFullName(), guest.getEmail(), guest.getPhone());
        }

        if (ticket.getUserId() == null) {
            return new CustomerInformation("UNKNOWN", null, null, null);
        }

        return userProfileRepository.findContactByUserId(ticket.getUserId())
                .map(contact -> new CustomerInformation("USER", contact.getFullname(), contact.getEmail(), contact.getPhone()))
                .orElseGet(() -> new CustomerInformation("USER", null, null, null));
    }

    private GuestInformation createGuestInformation(CreateWalkInServiceTicketRequest request) {
        if (!StringUtils.hasText(request.getGuestFullName())
                || !StringUtils.hasText(request.getGuestEmail())
                || !StringUtils.hasText(request.getGuestPhone())) {
            throw new CustomException(400, "Khách vãng lai cần nhập họ tên, email và số điện thoại");
        }

        return guestInformationRepository.save(GuestInformation.builder()
                .fullName(request.getGuestFullName().trim())
                .email(request.getGuestEmail().trim().toLowerCase())
                .phone(request.getGuestPhone().trim())
                .build());
    }

    private UUID getCurrentAdvisorDealershipId() {
        UUID currentUserId = getCurrentUserId();
        ServiceAdvisor advisor = serviceAdvisorRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(403, "Tài khoản cố vấn dịch vụ chưa được gán đại lý"));

        return advisor.getDealershipId();
    }

    private ServiceTicketStatus parseServiceTicketStatus(String status) {
        try {
            return ServiceTicketStatus.valueOf(status.trim().toUpperCase());
        }
        catch (IllegalArgumentException exception) {
            throw new CustomException(400, "Trạng thái phiếu dịch vụ không hợp lệ");
        }
    }

    private void validateTicketCanEditItems(ServiceTicket serviceTicket) {
        if (serviceTicket.getStatus() != ServiceTicketStatus.IN_PROGRESS) {
            throw new CustomException(400, "Chỉ có thể cập nhật hạng mục khi phiếu dịch vụ đang được sửa");
        }
    }

    private void validateServiceItemRequest(ServiceTicket serviceTicket, CreateServiceItemRequest request) {
        if (request.getItemType() == ServiceItemType.PART && request.getAccessoryId() != null) {
            Car car = findCar(serviceTicket.getVinId());
            if (car.getCarVersion() == null
                    || !carAccessoryRepository.existsByCarVersionIdAndAccessoryId(car.getCarVersion().getId(), request.getAccessoryId())) {
                throw new CustomException(400, "Phụ tùng không phù hợp với dòng xe của VIN này");
            }
        }
    }

    private void validateCurrentUserCanViewInvoice(ServiceTicket serviceTicket) {
        UUID currentUserId = getCurrentUserId();
        String role = SecurityContextUtil.getCurrentUserRole();

        if ("ROLE_MECHANIC".equals(role) && currentUserId.equals(serviceTicket.getMechanicId())) {
            return;
        }

        if ("ROLE_USER".equals(role) && currentUserId.equals(serviceTicket.getUserId())) {
            return;
        }

        if ("ROLE_SERVICE_ADVISOR".equals(role) && getCurrentAdvisorDealershipId().equals(serviceTicket.getDealershipId())) {
            return;
        }

        throw new CustomException(403, "Bạn không có quyền xem phiếu thu này");
    }

    private Car findCar(String vinId) {
        return carRepository.findById(vinId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy xe theo VIN"));
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

    // Phương thức tiện ích để xác định đơn giá của hạng mục dịch vụ. WARRANTY/GIFT vẫn giữ đơn giá gốc để đối chiếu, chỉ finalPrice bằng 0.
    private BigDecimal resolveUnitPrice(CreateServiceItemRequest request) {
        if (request.getUnitPrice() == null) {
            if (request.getBillingType() == BillingType.WARRANTY || request.getBillingType() == BillingType.GIFT) {
                return BigDecimal.ZERO;
            }
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

    private String normalizeVin(String vinId) {
        String normalized = normalizeRequired(vinId, "Số VIN không được để trống").toUpperCase();
        if (normalized.length() != 17) {
            throw new CustomException(400, "Số VIN phải gồm 17 ký tự");
        }
        return normalized;
    }

    private record CustomerInformation(String customerType, String fullName, String email, String phone) {
    }
}
