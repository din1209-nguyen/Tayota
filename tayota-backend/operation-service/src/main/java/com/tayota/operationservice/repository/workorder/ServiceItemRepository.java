package com.tayota.operationservice.repository.workorder;

import com.tayota.operationservice.entity.workorder.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {

    // Lấy tất cả các ServiceItem liên quan đến một ServiceTicket cụ thể, sắp xếp theo thời gian tạo
    List<ServiceItem> findByServiceTicketId(UUID serviceTicketId);

    // Lấy tất cả các ServiceItem liên quan đến một ServiceTicket cụ thể, sắp xếp theo thời gian tạo tăng dần
    List<ServiceItem> findByServiceTicketIdOrderByCreatedAtAsc(UUID serviceTicketId);
}
