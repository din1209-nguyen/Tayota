package com.tayota.userservice.object;

import com.tayota.userservice.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterCacheData {
    UserProfile userProfile;
    String token;
}
