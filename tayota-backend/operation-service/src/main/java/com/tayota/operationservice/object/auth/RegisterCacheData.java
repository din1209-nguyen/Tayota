package com.tayota.operationservice.object.auth;

import com.tayota.operationservice.entity.user.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterCacheData {
    UserProfile userProfile;
    String token;
}
