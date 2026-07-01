package com.tayota.operationservice.repository.user;

import com.tayota.operationservice.entity.user.ServiceAdvisor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Repository này dùng để quản lý thông tin về Service Advisor, mỗi Service Advisor sẽ thuộc về một đại lý cụ thể (dealershipId)
public interface ServiceAdvisorRepository extends JpaRepository<ServiceAdvisor, UUID> {
}