package com.nguyendin.operationservice.repository;

import com.nguyendin.operationservice.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
    List<ServiceItem> findByServiceTicketId(UUID serviceTicketId);
}