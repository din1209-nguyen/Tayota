package com.tayota.userservice.controller;

import com.tayota.userservice.dto.Request.*;
import com.tayota.userservice.dto.Response.AccessTokenResponseDTO;
import com.tayota.userservice.dto.Response.DeviceResponseDTO;
import com.tayota.userservice.dto.Request.ForgotPasswordResetRequestDTO;
import com.tayota.userservice.dto.Response.TokenForResetPasswordResponseDTO;
import com.tayota.userservice.enums.StatusType;
import com.tayota.userservice.object.TokenPair;
import com.tayota.userservice.service.AuthService;
import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.userservice.util.CookieUtil;
import com.tayota.userservice.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    // Tạo tài khoản
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create-account")
    public ApiResponse<Void> createAccount(@Valid @RequestBody CreateAccountRequestDTO createAccountRequestDTO) {
        authService.createAccount(createAccountRequestDTO);
        return ApiResponse.success(200, "Tạo tài khoản thành công!", null);
    }

    // Đăng ký tài khoản
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO, HttpServletRequest request) {
        authService.register(registerRequestDTO, IpUtil.getClientIp(request));
        return ApiResponse.success(200, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.", null);
    }

    // Xác thực tài khoản qua email và token
    @PostMapping("/verify-account")
    public ApiResponse<Void> verifyAccount(@Valid @RequestBody VerifyAccountRequestDTO verifyEmailRequestDTO) {
        authService.verifyAccount(verifyEmailRequestDTO);
        return ApiResponse.success(200, "Email đã xác thực thành công!", null);
    }

    // Đăng nhập tài khoản
    @PostMapping("/login")
    public ApiResponse<AccessTokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO, HttpServletRequest request, HttpServletResponse response) {
        TokenPair tokenPair = authService.login(
                loginRequestDTO,
                IpUtil.getClientIp(request),
                request.getHeader("User-Agent"),
                cookieUtil.getCookieValue(request, "refresh_token")
        );

        /* Bước 9: Chỉ lưu refresh-token vào HttpOnly Cookie, access-token trả về body để client gửi qua Authorization header */
        cookieUtil.setRefreshTokenCookie(response, tokenPair.getRefreshToken());
        return ApiResponse.success(200, "Đăng nhập thành công!", new AccessTokenResponseDTO(tokenPair.getAccessToken()));
    }

    // Đăng nhập tài khoản bằng Google
    @PostMapping("/oauth/google")
    public ApiResponse<AccessTokenResponseDTO> googleLogin(
            @Valid @RequestBody GoogleLoginRequestDTO googleLoginRequestDTO,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        TokenPair tokenPair = authService.loginWithGoogle(
                googleLoginRequestDTO,
                IpUtil.getClientIp(request),
                request.getHeader("User-Agent"),
                cookieUtil.getCookieValue(request, "refresh_token")
        );

        /* Bước 9: Chỉ lưu refresh-token vào HttpOnly Cookie, access-token trả về body để client gửi qua Authorization header */
        cookieUtil.setRefreshTokenCookie(response, tokenPair.getRefreshToken());
        return ApiResponse.success(200, "Đăng nhập Google thành công!", new AccessTokenResponseDTO(tokenPair.getAccessToken()));
    }

    // Làm mới access-token bằng refresh-token
    @PostMapping("/refresh-token")
    public ApiResponse<AccessTokenResponseDTO> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            TokenPair newTokenPair = authService.refreshToken(
                    IpUtil.getClientIp(request),
                    request.getHeader("User-Agent"),
                    cookieUtil.getCookieValue(request, "refresh_token")
            );

            // Cập nhật refresh-token trong HttpOnly Cookie, access-token trả về body
            cookieUtil.setRefreshTokenCookie(response, newTokenPair.getRefreshToken());
            return ApiResponse.success(200, "Làm mới token thành công!", new AccessTokenResponseDTO(newTokenPair.getAccessToken()));
        }
        catch (Exception e) {
            // Xoá cookie chứa token khỏi trình duyệt nếu có bất kỳ lỗi nào xảy ra trong quá trình làm mới token
            // ví dụ: refresh token hết hạn, bị thu hồi, hoặc không hợp lệ,...
            cookieUtil.clearRefreshTokenCookie(response);
            throw e;
        }
    }

    // Đăng xuất tài khoản
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            authService.logout(cookieUtil.getCookieValue(request, "refresh_token"));
            return ApiResponse.success(200, "Đăng xuất thành công!", null);
        }
        finally {
            // Dù đăng xuất thành công hay thất bại thì cũng xoá cookie chứa token khỏi trình duyệt
            cookieUtil.clearRefreshTokenCookie(response);
        }
    }

    // Đăng xuất tất cả tài khoản trừ thiết bị hiện tại
    @PostMapping("/logout-all")
    public ApiResponse<AccessTokenResponseDTO> logoutAll(HttpServletRequest request, HttpServletResponse response) {
        TokenPair newTokenPair = authService.logoutAll(
                IpUtil.getClientIp(request),
                request.getHeader("User-Agent"),
                cookieUtil.getCookieValue(request, "refresh_token")
        );

        // Cập nhật refresh-token trong Cookie và trả access-token mới về body cho thiết bị hiện tại
        cookieUtil.setRefreshTokenCookie(response, newTokenPair.getRefreshToken());
        return ApiResponse.success(200, "Đăng xuất tất cả các thiết bị thành công!", new AccessTokenResponseDTO(newTokenPair.getAccessToken()));
    }

    // Gửi mã OTP để xác nhận cho tài khoản đã quên mật khẩu
    @PostMapping("/forgot-password/send-otp")
    public ApiResponse<Void> sendForgotPasswordOTP(
            @RequestParam(name = "email") @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") String email,
            HttpServletRequest request
    ) {
        authService.sendForgotPasswordOTP(email, IpUtil.getClientIp(request));
        return ApiResponse.success(200, "Mã OTP đã được gửi đến email của bạn!", null);
    }

    // Xác thực mã OTP cho tài khoản đã quên mật khẩu
    @PostMapping("/forgot-password/verify-otp")
    public ApiResponse<TokenForResetPasswordResponseDTO> verifyForgotPasswordOTP(
            @Valid @RequestBody VerifyForgotPasswordOTPRequestDTO verifyForgotPasswordOTPRequestDTO,
            HttpServletRequest request
    ) {
        String token = authService.verifyForgotPasswordOTP(verifyForgotPasswordOTPRequestDTO, IpUtil.getClientIp(request));
        return ApiResponse.success(200, "Xác thực OTP thành công! Bạn có thể tiếp tục đặt lại mật khẩu mới.", new TokenForResetPasswordResponseDTO(token));
    }

    // Đặt lại mật khẩu mới cho tài khoản đã quên mật khẩu
    @PatchMapping("/forgot-password/reset-password")
    public ApiResponse<Void> resetPasswordByForgotPassword(
            @Valid @RequestBody ForgotPasswordResetRequestDTO forgotPasswordResetRequestDTO,
            HttpServletRequest request
    ) {
        authService.resetPasswordByForgotPassword(forgotPasswordResetRequestDTO, IpUtil.getClientIp(request));
        return ApiResponse.success(200, "Đặt lại mật khẩu thành công!", null);
    }

    // Gửi mã OTP để xác nhận thay đổi mật khẩu
    @PostMapping("/change-password/send-otp")
    public ApiResponse<Void> sendChangePasswordOTP(HttpServletRequest request) {
        authService.sendChangePasswordOTP(IpUtil.getClientIp(request));
        return ApiResponse.success(200, "Mã OTP đã được gửi đến email của bạn!", null);
    }

    // Xác thực mã OTP cho tài khoản thay đổi mật khẩu
    @PostMapping("/change-password/verify-otp")
    public ApiResponse<TokenForResetPasswordResponseDTO> verifyChangePasswordOTP(
            @Valid @RequestBody VerifyChangePasswordOTPRequestDTO verifyChangePasswordOTPRequestDTO,
            HttpServletRequest request
    ) {
        String token = authService.verifyChangePasswordOTP(verifyChangePasswordOTPRequestDTO, IpUtil.getClientIp(request));
        return ApiResponse.success(200, "Xác thực OTP thành công! Bạn có thể tiếp tục đặt lại mật khẩu mới.", new TokenForResetPasswordResponseDTO(token));
    }

    // Đặt lại mật khẩu mới cho tài khoản thay đổi mật khẩu
    @PatchMapping("/change-password/reset-password")
    public ApiResponse<Void> resetPasswordByChangePassword(
            @Valid @RequestBody ChangePasswordResetRequestDTO changePasswordResetRequestDTO,
            HttpServletRequest request
    ) {
        authService.resetPasswordByChangePassword(changePasswordResetRequestDTO, IpUtil.getClientIp(request));
        return ApiResponse.success(200, "Đặt lại mật khẩu thành công!", null);
    }

    // Lấy danh sách thiết bị đã đăng nhập của một tài khoản
    @GetMapping("/devices/{userId}")
    public ApiResponse<List<DeviceResponseDTO>> getDevices(@PathVariable String userId) {
        List<DeviceResponseDTO> devices = authService.getDevices(userId);
        return ApiResponse.success(200, "Lấy danh sách thiết bị thành công!", devices);
    }

    // Thu hồi quyền truy cập của một thiết bị
    @DeleteMapping("/revoke/{userId}/{deviceId}")
    public ApiResponse<Void> revoke(@PathVariable String userId, @PathVariable String deviceId, HttpServletRequest request) {
        authService.revoke(userId, deviceId, cookieUtil.getCookieValue(request, "refresh_token"));
        return ApiResponse.success(200, "Thiết bị đã được thu hồi thành công!", null);
    }

    // Khoá tài khoản
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PatchMapping("/ban/{userId}")
    public ApiResponse<Void> banAccount(@PathVariable String userId) {
        authService.changeUserStatus(userId, StatusType.BANNED);
        return ApiResponse.success(200, "Tài khoản đã bị khoá thành công!", null);
    }

    // Mở khoá tài khoản
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PatchMapping("/unban/{userId}")
    public ApiResponse<Void> unbanAccount(@PathVariable String userId) {
        authService.changeUserStatus(userId, StatusType.ACTIVE);
        return ApiResponse.success(200, "Tài khoản đã được mở khoá thành công!", null);
    }
}