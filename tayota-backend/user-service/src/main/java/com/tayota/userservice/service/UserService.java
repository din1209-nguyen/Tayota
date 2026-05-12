package com.tayota.userservice.service;

import com.tayota.commoncore.dto.ErrorCode;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.commoncore.util.SecurityContextUtil;
import com.tayota.userservice.dto.Request.UserProfileUpdateRequestDTO;
import com.tayota.userservice.entity.UserProfile;
import com.tayota.userservice.repository.UserProfileRepository;
import com.tayota.userservice.dto.Response.BasicInformationResponseDTO;
import com.tayota.userservice.dto.Response.UserProfileResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserProfileRepository userProfileRepository;

    // Lấy thông tin cơ bản
    public BasicInformationResponseDTO getBasicInformation() {
        // Lấy userId của user hiện tại từ SecurityContext
        String userId = SecurityContextUtil.getCurrentUserId();

        // Lấy user profile từ csdl
        UserProfile userProfile = userProfileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Trả về thông tin cơ bản
        return new BasicInformationResponseDTO(userProfile.getFullname(), userProfile.getAvatarUrl());
    }

    // Lấy hồ sơ
    public UserProfileResponseDTO getProfile(String userId) {
        // Kiểm tra userId không được bỏ trống
        if (!StringUtils.hasText(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // Lấy thông tin user từ SecurityContext
        String currentUserId = SecurityContextUtil.getCurrentUserId();
        String currentUserRole = SecurityContextUtil.getCurrentUserRole();

        // Lấy user mục tiêu từ csdl để kiểm tra role
        UserProfile targetUser = userProfileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Nếu userId khác với id của tài khoản đang đăng nhập thì kiểm tra quyền thực hiện hợp lê
        if (!currentUserId.equals(userId) && !SecurityContextUtil.validateRoleSuperiority(currentUserRole, targetUser.getUser().getRole().name())) {
            throw new CustomException(403, "Không thể thực hiện thao tác trên tài khoản này.");
        }

        // Trả về hồ sơ người dùng
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

    // Cập nhật hồ sơ
    public void updateProfile(UserProfileUpdateRequestDTO userProfileUpdateRequestDTO) {
        // Lấy thông tin của user hiện tại từ SecurityContext
        String currentUserId = SecurityContextUtil.getCurrentUserId();
        String currentUserRole = SecurityContextUtil.getCurrentUserRole();

        // Kiểm tra user có tồn tại không
        UserProfile existingProfile = userProfileRepository.findById(UUID.fromString(userProfileUpdateRequestDTO.getUserId()))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Nếu userId khác với id của tài khoản đang đăng nhập thì kiểm tra quyền thực hiện hợp lê
        if (!currentUserId.equals(userProfileUpdateRequestDTO.getUserId()) && !SecurityContextUtil.validateRoleSuperiority(currentUserRole, existingProfile.getUser().getRole().name())) {
            throw new CustomException(403, "Không thể thực hiện thao tác trên tài khoản này.");
        }

        // Cập nhật những trường được truyền vào từ request
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

        // Lưu vào csdl
        userProfileRepository.save(existingProfile);
    }
}
