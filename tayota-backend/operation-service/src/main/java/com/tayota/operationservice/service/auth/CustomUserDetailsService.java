package com.tayota.operationservice.service.auth;

import com.tayota.operationservice.dto.common.ErrorCode;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.enums.user.ProviderType;
import com.tayota.operationservice.object.auth.CustomUserDetails;
import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @NullMarked
    public UserDetails loadUserByUsername(String email)  {
        // Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(ErrorCode.USER_NOT_FOUND.getMessage()));

        // Kiểm tra trạng thái người dùng
        if (user.getStatus() == StatusType.BANNED) {
            throw new CustomException(403, "Tài khoản đã bị khóa!");
        }

        // Kiểm tra người dùng đăng nhập bằng Google nhưng lại dùng email/password để đăng nhập
        if (user.getLoginProvider() == ProviderType.GOOGLE) {
            throw new CustomException(400, "Tài khoản này được đăng nhập qua Google, vui lòng sử dụng Google để đăng nhập!");
        }

        return new CustomUserDetails(user);
    }
}

