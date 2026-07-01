package com.tayota.operationservice.service.car;

import com.tayota.operationservice.dto.request.car.DealershipRequestDTO;
import com.tayota.operationservice.dto.response.car.DealershipResponseDTO;
import com.tayota.operationservice.entity.car.Dealership;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.repository.car.DealershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealershipService {
    private final DealershipRepository dealershipRepository;

    @Transactional(readOnly = true)
    public List<DealershipResponseDTO> getActiveDealerships() {
        return dealershipRepository.findByIsActiveTrueOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DealershipResponseDTO> getDealershipsForManagement() {
        return dealershipRepository.findAll(Sort.by("name")).stream().map(this::toResponse).toList();
    }

    @Transactional
    public DealershipResponseDTO createDealership(DealershipRequestDTO request) {
        Dealership dealership = Dealership.builder()
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .placeId(request.getPlaceId())
                .phone(request.getPhone())
                .operatingHours(request.getOperatingHours())
                .isActive(request.getActive() == null || request.getActive())
                .build();
        return toResponse(dealershipRepository.save(dealership));
    }

    @Transactional
    public DealershipResponseDTO updateDealership(String dealershipId, DealershipRequestDTO request) {
        Dealership dealership = findDealership(dealershipId);
        dealership.setName(request.getName());
        dealership.setAddress(request.getAddress());
        dealership.setLatitude(request.getLatitude());
        dealership.setLongitude(request.getLongitude());
        dealership.setPlaceId(request.getPlaceId());
        dealership.setPhone(request.getPhone());
        dealership.setOperatingHours(request.getOperatingHours());
        if (request.getActive() != null) {
            dealership.setActive(request.getActive());
        }
        return toResponse(dealershipRepository.save(dealership));
    }

    @Transactional
    public void deactivateDealership(String dealershipId) {
        Dealership dealership = findDealership(dealershipId);
        dealership.setActive(false);
        dealershipRepository.save(dealership);
    }

    private Dealership findDealership(String value) {
        try {
            return dealershipRepository.findById(UUID.fromString(value))
                    .orElseThrow(() -> new CustomException(404, "Không tìm thấy đại lý."));
        } catch (IllegalArgumentException exception) {
            throw new CustomException(400, "Id đại lý không hợp lệ.");
        }
    }

    private DealershipResponseDTO toResponse(Dealership dealership) {
        return new DealershipResponseDTO(
                dealership.getId(), dealership.getName(), dealership.getAddress(), dealership.getPhone(),
                dealership.getOperatingHours(), dealership.getLatitude(), dealership.getLongitude(),
                dealership.getPlaceId(), dealership.isActive()
        );
    }
}
