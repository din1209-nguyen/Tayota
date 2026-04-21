package com.tayota.userservice.service;

import com.tayota.commoncore.dto.ErrorCode;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.entity.CustomUserDetails;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.enums.StatusType;
import com.tayota.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

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
        if (user.getStatus() == StatusType.UNVERIFIED) {
            throw new CustomException(403, "Tài khoản chưa được xác thực. Vui lòng kiểm tra email để xác thực tài khoản!");
        }
        else if (user.getStatus() == StatusType.BANNED) {
            throw new CustomException(403, "Tài khoản đã bị khóa!");
        }

        return new CustomUserDetails(user);
    }
}

