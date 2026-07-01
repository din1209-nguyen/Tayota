package com.tayota.operationservice.object.auth;

import com.tayota.operationservice.entity.user.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCacheData {
    UserProfile userProfile;
    String token;
}
