package com.tayota.operationservice.repository.workorder;


import com.tayota.operationservice.entity.workorder.ServiceTicket;
import com.tayota.operationservice.enums.workorder.ServiceTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceTicketRepository extends JpaRepository<ServiceTicket, UUID> {
    Optional<ServiceTicket> findByAppointmentId(UUID appointmentId);

    List<ServiceTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ServiceTicket> findByMechanicIdOrderByCreatedAtDesc(UUID mechanicId);

    List<ServiceTicket> findByDealershipIdOrderByCreatedAtDesc(UUID dealershipId);

    List<ServiceTicket> findByDealershipIdAndStatusOrderByCreatedAtDesc(UUID dealershipId, ServiceTicketStatus status);

    List<ServiceTicket> findByMechanicIdAndStatusInOrderByCreatedAtDesc(
            UUID mechanicId,
            List<ServiceTicketStatus> statuses
    );

    List<ServiceTicket> findByStatusOrderByCreatedAtDesc(ServiceTicketStatus status);

    @Query("""
            select ticket
            from ServiceTicket ticket
            where ticket.dealershipId = :dealershipId
              and ticket.createdAt >= :fromInclusive
              and ticket.createdAt < :toExclusive
            order by ticket.createdAt desc
            """)
    List<ServiceTicket> findAdvisorReportTicketsByCreatedAt(
            @Param("dealershipId") UUID dealershipId,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            select ticket
            from ServiceTicket ticket
            where ticket.dealershipId = :dealershipId
              and ticket.status = :status
              and ticket.completedAt >= :fromInclusive
              and ticket.completedAt < :toExclusive
            order by ticket.completedAt desc
            """)
    List<ServiceTicket> findAdvisorReportCompletedTickets(
            @Param("dealershipId") UUID dealershipId,
            @Param("status") ServiceTicketStatus status,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );
}
