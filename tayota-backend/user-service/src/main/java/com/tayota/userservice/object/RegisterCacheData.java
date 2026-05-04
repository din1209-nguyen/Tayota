package com.tayota.userservice.object;

import com.tayota.userservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterCacheData {
    User user;
    String token;
}
