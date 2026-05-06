package com.tayota.userservice.service;

import com.tayota.commoncore.dto.ErrorCode;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.enums.ProviderType;
import com.tayota.userservice.object.CustomUserDetails;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.enums.StatusType;
import com.tayota.userservice.repository.UserRepository;
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

