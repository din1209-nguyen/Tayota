package com.tayota.operationservice.service.user;

import com.tayota.operationservice.dto.common.ErrorCode;
import com.tayota.operationservice.dto.request.admin.AdminUpdateDealershipRequest;
import com.tayota.operationservice.dto.request.user.UserProfileUpdateRequestDTO;
import com.tayota.operationservice.dto.response.admin.AdminUserResponse;
import com.tayota.operationservice.dto.response.admin.ManagerUserStatsResponse;
import com.tayota.operationservice.dto.response.car.PaginationResponseDTO;
import com.tayota.operationservice.dto.response.user.BasicInformationResponseDTO;
import com.tayota.operationservice.dto.response.user.AdvisorCustomerResponse;
import com.tayota.operationservice.dto.response.user.UserProfileResponseDTO;
import com.tayota.operationservice.entity.user.ServiceAdvisor;
import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.entity.user.UserProfile;
import com.tayota.operationservice.entity.car.Dealership;
import com.tayota.operationservice.entity.workorder.Mechanic;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.workorder.MechanicRepository;
import com.tayota.operationservice.repository.car.DealershipRepository;
import com.tayota.operationservice.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final EnumSet<RoleType> MANAGER_TARGET_ROLES =
            EnumSet.of(RoleType.SERVICE_ADVISOR, RoleType.ASSISTANT, RoleType.MECHANIC, RoleType.USER);
    private final UserProfileRepository userProfileRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final MechanicRepository mechanicRepository;
    private final DealershipRepository dealershipRepository;

    @Transactional(readOnly = true)
    public PaginationResponseDTO<AdminUserResponse> searchUsers(
            String keyword,
            RoleType role,
            StatusType status,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size));
        Page<UserProfile> result = StringUtils.hasText(keyword)
                ? userProfileRepository.searchForAdminWithKeyword(keyword.trim().toLowerCase(), role, status, pageable)
                : userProfileRepository.searchForAdminWithoutKeyword(role, status, pageable);

        return new PaginationResponseDTO<>(
                result.getContent().stream().map(this::toAdminResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserForAdmin(String userId) {
        return toAdminResponse(findProfile(userId));
    }

    @Transactional(readOnly = true)
    public PaginationResponseDTO<AdminUserResponse> searchUsersForManager(
            String keyword, RoleType role, StatusType status, int page, int size
    ) {
        validateManagerRoleFilter(role);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size));
        Page<UserProfile> result = StringUtils.hasText(keyword)
                ? userProfileRepository.searchForManagerWithKeyword(MANAGER_TARGET_ROLES, keyword.trim().toLowerCase(), role, status, pageable)
                : userProfileRepository.searchForManagerWithoutKeyword(MANAGER_TARGET_ROLES, role, status, pageable);
        return new PaginationResponseDTO<>(
                result.getContent().stream().map(this::toAdminResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserForManager(String userId) {
        UserProfile profile = findProfile(userId);
        validateManagerTarget(profile.getUser());
        return toAdminResponse(profile);
    }

    @Transactional(readOnly = true)
    public ManagerUserStatsResponse getManagerUserStats() {
        Map<String, Long> byRole = Map.of(
                RoleType.SERVICE_ADVISOR.name(), countManagerUsers(RoleType.SERVICE_ADVISOR, null),
                RoleType.ASSISTANT.name(), countManagerUsers(RoleType.ASSISTANT, null),
                RoleType.MECHANIC.name(), countManagerUsers(RoleType.MECHANIC, null),
                RoleType.USER.name(), countManagerUsers(RoleType.USER, null)
        );
        Map<String, Long> byStatus = Map.of(
                StatusType.ACTIVE.name(), countManagerUsers(null, StatusType.ACTIVE),
                StatusType.BANNED.name(), countManagerUsers(null, StatusType.BANNED)
        );
        long total = byRole.values().stream().mapToLong(Long::longValue).sum();
        return new ManagerUserStatsResponse(total, byRole, byStatus);
    }

    @Transactional(readOnly = true)
    public List<AdvisorCustomerResponse> searchActiveCustomersForAdvisor(String keyword, int size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
        if (!StringUtils.hasText(normalizedKeyword)) {
            return List.of();
        }

        return userProfileRepository
                .searchActiveCustomersForAdvisor(
                        normalizedKeyword,
                        RoleType.USER,
                        StatusType.ACTIVE,
                        PageRequest.of(0, normalizeSize(size))
                )
                .stream()
                .map(profile -> new AdvisorCustomerResponse(
                        profile.getId(),
                        profile.getFullname(),
                        profile.getUser().getEmail(),
                        profile.getPhone()
                ))
                .toList();
    }

    @Transactional
    public AdminUserResponse updateDealershipForAdmin(String userId, AdminUpdateDealershipRequest request) {
        UserProfile targetProfile = findProfile(userId);
        User targetUser = targetProfile.getUser();

        if (!SecurityContextUtil.validateRoleSuperiority(
                SecurityContextUtil.getCurrentUserRole(),
                targetUser.getRole().name()
        )) {
            throw new CustomException(403, "Không thể thực hiện thao tác trên tài khoản này.");
        }

        if (targetUser.getRole() != RoleType.SERVICE_ADVISOR && targetUser.getRole() != RoleType.MECHANIC) {
            throw new CustomException(400, "Chỉ có thể cập nhật đại lý cho cố vấn dịch vụ hoặc kỹ thuật viên.");
        }

        UUID dealershipId;
        try {
            dealershipId = UUID.fromString(request.getDealershipId());
        }
        catch (IllegalArgumentException exception) {
            throw new CustomException(400, "Đại lý không hợp lệ.");
        }

        Dealership dealership = dealershipRepository.findById(dealershipId)
                .filter(Dealership::isActive)
                .orElseThrow(() -> new CustomException(400, "Đại lý không tồn tại hoặc đã ngừng hoạt động."));

        if (targetUser.getRole() == RoleType.SERVICE_ADVISOR) {
            ServiceAdvisor advisor = serviceAdvisorRepository.findById(targetUser.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            advisor.setDealershipId(dealership.getId());
            serviceAdvisorRepository.save(advisor);
        }
        else {
            Mechanic mechanic = mechanicRepository.findById(targetUser.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            mechanic.setDealershipId(dealership.getId());
            mechanicRepository.save(mechanic);
        }

        return toAdminResponse(targetProfile);
    }

    public BasicInformationResponseDTO getBasicInformation() {
        String userId = SecurityContextUtil.getCurrentUserId();
        UserProfile userProfile = userProfileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new BasicInformationResponseDTO(
                userProfile.getId(),
                userProfile.getFullname(),
                userProfile.getAvatarUrl(),
                userProfile.getUser().getEmail(),
                userProfile.getUser().getRole().name()
        );
    }

    public UserProfileResponseDTO getProfile(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String currentUserId = SecurityContextUtil.getCurrentUserId();
        String currentUserRole = SecurityContextUtil.getCurrentUserRole();
        UserProfile targetUser = userProfileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!currentUserId.equals(userId)
                && !SecurityContextUtil.validateRoleSuperiority(currentUserRole, targetUser.getUser().getRole().name())) {
            throw new CustomException(403, "Không thể thực hiện thao tác trên tài khoản này.");
        }

        return new UserProfileResponseDTO(
                targetUser.getId(),
                targetUser.getFullname(),
                targetUser.getPhone(),
                targetUser.getGender(),
                targetUser.getBirthDate(),
                targetUser.getAddress(),
                targetUser.getAvatarUrl(),
                targetUser.getUser().getEmail(),
                targetUser.getUser().getCreatedAt()
        );
    }

    public void updateProfile(UserProfileUpdateRequestDTO userProfileUpdateRequestDTO) {
        String currentUserId = SecurityContextUtil.getCurrentUserId();
        String currentUserRole = SecurityContextUtil.getCurrentUserRole();
        UserProfile existingProfile = userProfileRepository.findById(UUID.fromString(userProfileUpdateRequestDTO.getUserId()))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!currentUserId.equals(userProfileUpdateRequestDTO.getUserId())
                && !SecurityContextUtil.validateRoleSuperiority(currentUserRole, existingProfile.getUser().getRole().name())) {
            throw new CustomException(403, "Không thể thực hiện thao tác trên tài khoản này.");
        }

        if (userProfileUpdateRequestDTO.getFullname() != null) {
            existingProfile.setFullname(userProfileUpdateRequestDTO.getFullname());
        }
        if (userProfileUpdateRequestDTO.getPhone() != null) {
            existingProfile.setPhone(userProfileUpdateRequestDTO.getPhone());
        }
        if (userProfileUpdateRequestDTO.getGender() != null) {
            existingProfile.setGender(userProfileUpdateRequestDTO.getGender());
        }
        if (userProfileUpdateRequestDTO.getBirthDate() != null) {
            existingProfile.setBirthDate(userProfileUpdateRequestDTO.getBirthDate());
        }
        if (userProfileUpdateRequestDTO.getAddress() != null) {
            existingProfile.setAddress(userProfileUpdateRequestDTO.getAddress());
        }
        if (userProfileUpdateRequestDTO.getAvatarUrl() != null) {
            existingProfile.setAvatarUrl(userProfileUpdateRequestDTO.getAvatarUrl());
        }

        userProfileRepository.save(existingProfile);
    }

    private UserProfile findProfile(String userId) {
        try {
            return userProfileRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        }
        catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private AdminUserResponse toAdminResponse(UserProfile profile) {
        User user = profile.getUser();
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getLoginProvider(),
                profile.getFullname(),
                profile.getPhone(),
                profile.getAvatarUrl(),
                findDealershipId(user),
                user.getCreatedAt()
        );
    }

    private UUID findDealershipId(User user) {
        if (user.getRole() == RoleType.SERVICE_ADVISOR) {
            return serviceAdvisorRepository.findById(user.getId())
                    .map(ServiceAdvisor::getDealershipId)
                    .orElse(null);
        }
        if (user.getRole() == RoleType.MECHANIC) {
            return mechanicRepository.findById(user.getId())
                    .map(Mechanic::getDealershipId)
                    .orElse(null);
        }
        return null;
    }

    private void validateManagerRoleFilter(RoleType role) {
        if (role != null && !MANAGER_TARGET_ROLES.contains(role)) {
            throw new CustomException(403, "Manager chỉ được xem tài khoản cấp dưới.");
        }
    }

    private void validateManagerTarget(User user) {
        if (!MANAGER_TARGET_ROLES.contains(user.getRole())) {
            throw new CustomException(403, "Manager chỉ được xem tài khoản cấp dưới.");
        }
    }

    private long countManagerUsers(RoleType role, StatusType status) {
        return userProfileRepository.searchForManagerWithoutKeyword(
                MANAGER_TARGET_ROLES, role, status, PageRequest.of(0, 1)
        ).getTotalElements();
    }

    private int normalizeSize(int size) {
        if (size <= 0) return 10;
        return Math.min(size, 100);
    }
}
