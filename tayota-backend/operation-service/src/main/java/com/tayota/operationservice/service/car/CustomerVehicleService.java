package com.tayota.operationservice.service.car;

import com.tayota.operationservice.dto.request.car.AssignCustomerVehicleRequest;
import com.tayota.operationservice.dto.response.car.CustomerVehicleResponse;
import com.tayota.operationservice.entity.car.Car;
import com.tayota.operationservice.entity.user.ServiceAdvisor;
import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.entity.user.UserProfile;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.repository.car.CarRepository;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.user.UserRepository;
import com.tayota.operationservice.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerVehicleService {
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;

    @Transactional
    public CustomerVehicleResponse assignVehicleToCustomer(AssignCustomerVehicleRequest request) {
        UUID advisorDealershipId = getCurrentAdvisorDealershipId();
        User customer = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy khách hàng"));

        if (customer.getRole() != RoleType.USER || customer.getStatus() != StatusType.ACTIVE) {
            throw new CustomException(400, "Chỉ có thể gán xe cho tài khoản khách hàng đang hoạt động");
        }

        Car car = findCarForAdvisor(normalizeVin(request.getVinId()), advisorDealershipId);
        if (car.getOwnerUserId() != null && !car.getOwnerUserId().equals(customer.getId())) {
            throw new CustomException(409, "VIN này đã được gán cho khách hàng khác");
        }

        car.setOwnerUserId(customer.getId());

        return toResponse(carRepository.save(car));
    }

    @Transactional(readOnly = true)
    public List<CustomerVehicleResponse> getMyVehicles() {
        UUID userId = UUID.fromString(SecurityContextUtil.getCurrentUserId());
        return carRepository.findByOwnerUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerVehicleResponse> getCustomerVehicles(UUID userId) {
        UUID advisorDealershipId = getCurrentAdvisorDealershipId();
        return carRepository.findByOwnerUserId(userId)
                .stream()
                .filter(car -> car.getDealership() != null && advisorDealershipId.equals(car.getDealership().getId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void removeVehicleFromCustomer(String vinId) {
        UUID advisorDealershipId = getCurrentAdvisorDealershipId();
        Car car = findCarForAdvisor(normalizeVin(vinId), advisorDealershipId);
        car.setOwnerUserId(null);
        carRepository.save(car);
    }

    private Car findCarForAdvisor(String vinId, UUID advisorDealershipId) {
        Car car = carRepository.findById(vinId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy xe theo VIN"));

        if (car.getDealership() == null || !advisorDealershipId.equals(car.getDealership().getId())) {
            throw new CustomException(403, "Bạn không có quyền gán xe của đại lý này");
        }

        return car;
    }

    private CustomerVehicleResponse toResponse(Car car) {
        UserProfile profile = car.getOwnerUserId() == null
                ? null
                : userProfileRepository.findById(car.getOwnerUserId()).orElse(null);

        return new CustomerVehicleResponse(
                car.getVinId(),
                car.getOwnerUserId(),
                profile == null ? null : profile.getFullname(),
                profile == null ? null : profile.getUser().getEmail(),
                profile == null ? null : profile.getPhone(),
                car.getCarVersion() == null ? null : car.getCarVersion().getId(),
                car.getCarVersion() == null ? null : car.getCarVersion().getName(),
                car.getDealership() == null ? null : car.getDealership().getId(),
                car.getStatus(),
                null
        );
    }

    private UUID getCurrentAdvisorDealershipId() {
        UUID currentUserId = UUID.fromString(SecurityContextUtil.getCurrentUserId());
        ServiceAdvisor advisor = serviceAdvisorRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(403, "Tài khoản cố vấn dịch vụ chưa được gán đại lý"));

        return advisor.getDealershipId();
    }

    private String normalizeVin(String vinId) {
        if (!StringUtils.hasText(vinId)) {
            throw new CustomException(400, "Số VIN không được để trống");
        }

        String normalized = vinId.trim().toUpperCase();
        if (normalized.length() != 17) {
            throw new CustomException(400, "Số VIN phải gồm 17 ký tự");
        }

        return normalized;
    }
}
