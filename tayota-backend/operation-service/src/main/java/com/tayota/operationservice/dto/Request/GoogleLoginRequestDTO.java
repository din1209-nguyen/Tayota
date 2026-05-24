package com.tayota.operationservice.dto.Request;

import com.tayota.commoncore.exception.CustomException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class GoogleLoginRequestDTO {
    // Lưu trữ Google ID Token (Thường dùng cho các client cũ hoặc Mobile App)
    private String idToken;

    // Lưu trữ credential token (Tên gọi mới của ID Token khi dùng thư viện Google Identity Services trên Web)
    private String credential;

    // Lấy ra token hợp lệ để xác thực
    public String getToken() {
        if (StringUtils.hasText(credential)) return credential;
        if (StringUtils.hasText(idToken)) return idToken;
        throw new CustomException(400, "Google ID token không được để trống!");
    }
}