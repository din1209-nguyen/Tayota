package com.tayota.authservice.mapper;

import com.tayota.authservice.dto.Request.RegisterRequestDTO;
import com.tayota.authservice.entity.User;
import com.tayota.authservice.enums.ProviderType;
import com.tayota.authservice.enums.RoleType;
import com.tayota.authservice.enums.StatusType;

public class UserMapper {
    public static User toEntity(RegisterRequestDTO registerRequestDTO, String newPasswordHash) {
        User newUser = new User();
        newUser.setEmail(registerRequestDTO.getEmail());
        newUser.setPasswordHash(newPasswordHash);
        newUser.setLoginProvider(ProviderType.LOCAL); // tạo tài khoảng bằng mật khẩu nên 'LOCAL'
        newUser.setProviderUserId(null); // tạo tài khoảng bằng mật khẩu nên 'null'
        newUser.setRole(RoleType.USER);
        newUser.setStatus(StatusType.UNVERIFIED); // tạo tài khoản chưa xác thực
        return newUser;
    }
}
