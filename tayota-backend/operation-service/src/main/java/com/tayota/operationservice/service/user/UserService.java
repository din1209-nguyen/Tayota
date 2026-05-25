package com.tayota.operationservice.service.user;

import com.tayota.operationservice.dto.common.ErrorCode;
import com.tayota.operationservice.dto.request.admin.AdminResetPasswordRequest;
import com.tayota.operationservice.dto.request.user.UserProfileUpdateRequestDTO;
import com.tayota.operationservice.dto.response.admin.AdminUserResponse;
import com.tayota.operationservice.dto.response.car.PaginationResponseDTO;
import com.tayota.operationservice.dto.response.user.BasicInformationResponseDTO;
import com.tayota.operationservice.dto.response.user.UserProfileResponseDTO;
import com.tayota.operationservice.entity.user.ServiceAdvisor;
import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.entity.user.UserProfile;
import com.tayota.operationservice.entity.workorder.Mechanic;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.workorder.MechanicRepository;
import com.tayota.operationservice.util.SecurityContextUtil;
import com.tayota.operationservice.util.SessionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserProfileRepository userProfileRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final MechanicRepository mechanicRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionUtil sessionUtil;

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

    @Transactional
    public void resetPasswordByAdmin(String userId, AdminResetPasswordRequest request) {
        UserProfile profile = findProfile(userId);
        profile.getUser().setPasswordHash(passwordEncoder.encode(request.getPassword()));
        sessionUtil.deleteAllSessions(userId);
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
            throw new CustomException(403, "Khong the thuc hien thao tac tren tai khoan nay.");
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
            throw new CustomException(403, "Khong the thuc hien thao tac tren tai khoan nay.");
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

    private int normalizeSize(int size) {
        if (size <= 0) return 10;
        return Math.min(size, 100);
    }
}
