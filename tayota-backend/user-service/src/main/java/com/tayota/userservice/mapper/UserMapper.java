package com.tayota.userservice.mapper;

import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.enums.ProviderType;
import com.tayota.commoncore.enums.RoleType;
import com.tayota.userservice.enums.StatusType;

public class UserMapper {
    public static User toEntity(RegisterRequestDTO registerRequestDTO, String newPasswordHash) {
        User newUser = new User();
        newUser.setEmail(registerRequestDTO.getEmail());
        newUser.setPasswordHash(newPasswordHash);
        newUser.setLoginProvider(ProviderType.LOCAL); // tạo tài khoảng bằng mật khẩu nên 'LOCAL'
        newUser.setProviderUserId(null); // tạo tài khoảng bằng mật khẩu nên 'null'
        newUser.setRole(RoleType.USER);
        newUser.setStatus(StatusType.ACTIVE); // tạo tài khoản chưa xác thực
        return newUser;
    }
}
