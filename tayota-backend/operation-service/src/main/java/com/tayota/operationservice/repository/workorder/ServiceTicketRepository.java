package com.tayota.operationservice.repository.workorder;


import com.tayota.operationservice.entity.workorder.ServiceTicket;
import com.tayota.operationservice.enums.workorder.ServiceTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceTicketRepository extends JpaRepository<ServiceTicket, UUID> {
    Optional<ServiceTicket> findByAppointmentId(UUID appointmentId);

    List<ServiceTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ServiceTicket> findByMechanicIdOrderByCreatedAtDesc(UUID mechanicId);

    List<ServiceTicket> findByMechanicIdAndStatusInOrderByCreatedAtDesc(
            UUID mechanicId,
            List<ServiceTicketStatus> statuses
    );

    List<ServiceTicket> findByStatusOrderByCreatedAtDesc(ServiceTicketStatus status);
}