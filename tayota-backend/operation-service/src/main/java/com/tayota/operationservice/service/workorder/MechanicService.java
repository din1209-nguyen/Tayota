package com.tayota.operationservice.service.workorder;

import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.util.SecurityContextUtil;
import com.tayota.operationservice.dto.response.workorder.MechanicResponse;
import com.tayota.operationservice.entity.user.ServiceAdvisor;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.workorder.MechanicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MechanicService {
    private final MechanicRepository mechanicRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;

    // Lấy danh sách thợ đang hoạt động thuộc đại lý của cố vấn dịch vụ hiện tại.
    @Transactional(readOnly = true)
    public List<MechanicResponse> getActiveMechanicsForMyDealership() {
        UUID dealershipId = getCurrentServiceAdvisorDealershipId();

        return mechanicRepository.findActiveMechanicsByDealershipId(dealershipId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UUID getCurrentServiceAdvisorDealershipId() {
        UUID currentUserId = UUID.fromString(SecurityContextUtil.getCurrentUserId());

        ServiceAdvisor serviceAdvisor = serviceAdvisorRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(403, "Tài khoản cố vấn dịch vụ chưa được gán đại lý"));

        return serviceAdvisor.getDealershipId();
    }

    private MechanicResponse toResponse(MechanicRepository.MechanicView mechanic) {
        return new MechanicResponse(
                mechanic.getId(),
                mechanic.getFullName(),
                mechanic.getSpecialty(),
                mechanic.getAverageRating()
        );
    }
}
