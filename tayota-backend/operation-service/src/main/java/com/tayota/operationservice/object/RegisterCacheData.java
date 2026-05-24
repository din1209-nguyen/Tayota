package com.tayota.operationservice.object;

import com.tayota.operationservice.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterCacheData {
    UserProfile userProfile;
    String token;
}
