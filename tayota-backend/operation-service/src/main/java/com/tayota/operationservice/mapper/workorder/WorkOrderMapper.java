package com.tayota.operationservice.mapper.workorder;

import com.tayota.operationservice.dto.response.workorder.ServiceItemResponse;
import com.tayota.operationservice.dto.response.workorder.ServiceTicketInfoResponse;
import com.tayota.operationservice.dto.response.workorder.ServiceTicketSummaryResponse;
import com.tayota.operationservice.entity.workorder.ServiceItem;
import com.tayota.operationservice.entity.workorder.ServiceTicket;
import org.springframework.stereotype.Component;

@Component
public class WorkOrderMapper {
    // Mapper để chuyển đổi giữa entity và DTO cho phiếu dịch vụ
    public ServiceTicketSummaryResponse toSummaryResponse(ServiceTicket serviceTicket) {
        return toSummaryResponse(serviceTicket, null, null, null, null);
    }

    public ServiceTicketSummaryResponse toSummaryResponse(
            ServiceTicket serviceTicket,
            String customerType,
            String customerFullName,
            String customerEmail,
            String customerPhone
    ) {
        return new ServiceTicketSummaryResponse(
                serviceTicket.getId(),
                serviceTicket.getAppointment() == null ? null : serviceTicket.getAppointment().getId(),
                serviceTicket.getGuestInformation() == null ? null : serviceTicket.getGuestInformation().getId(),
                serviceTicket.getVinId(),
                serviceTicket.getDealershipId(),
                serviceTicket.getMechanicId(),
                customerType,
                customerFullName,
                customerEmail,
                customerPhone,
                serviceTicket.getStatus(),
                serviceTicket.getReceivingAt(),
                serviceTicket.getTotalAmount()
        );
    }

    // Mapper để chuyển đổi giữa entity và DTO cho phiếu dịch vụ chi tiết
    public ServiceTicketInfoResponse toInfoResponse(ServiceTicket serviceTicket) {
        return toInfoResponse(serviceTicket, null, null, null, null);
    }

    public ServiceTicketInfoResponse toInfoResponse(
            ServiceTicket serviceTicket,
            String customerType,
            String customerFullName,
            String customerEmail,
            String customerPhone
    ) {
        return new ServiceTicketInfoResponse(
                serviceTicket.getId(),
                serviceTicket.getAppointment() == null ? null : serviceTicket.getAppointment().getId(),
                serviceTicket.getGuestInformation() == null ? null : serviceTicket.getGuestInformation().getId(),
                serviceTicket.getVinId(),
                serviceTicket.getDealershipId(),
                customerType,
                customerFullName,
                customerEmail,
                customerPhone,
                serviceTicket.getMechanicId(),
                serviceTicket.getMileageAtService(),
                serviceTicket.getStatus(),
                serviceTicket.getVehicleCondition(),
                serviceTicket.getNotes(),
                serviceTicket.getReceivingAt(),
                serviceTicket.getProcessingAt(),
                serviceTicket.getCompletedAt(),
                serviceTicket.getCanceledAt(),
                serviceTicket.getCancelReason(),
                serviceTicket.getTotalAmount()
        );
    }

    // Mapper để chuyển đổi giữa entity và DTO cho mục dịch vụ trong phiếu dịch vụ
    public ServiceItemResponse toServiceItemResponse(ServiceItem serviceItem) {
        return new ServiceItemResponse(
                serviceItem.getId(),
                serviceItem.getItemType(),
                serviceItem.getAccessoryId(),
                serviceItem.getItemName(),
                serviceItem.getQuantity(),
                serviceItem.getUnitPrice(),
                serviceItem.getBillingType(),
                serviceItem.getFinalPrice(),
                serviceItem.getNote()
        );
    }
}
