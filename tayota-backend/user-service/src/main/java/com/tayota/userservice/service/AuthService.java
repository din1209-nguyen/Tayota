package com.tayota.userservice.service;

import com.tayota.commoncore.dto.ErrorCode;
import com.tayota.userservice.dto.Request.LoginRequestDTO;
import com.tayota.userservice.model.TokenPair;
import com.tayota.userservice.model.CustomUserDetails;
import com.tayota.userservice.grpc.NotificationGrpcClient;
import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.enums.StatusType;
import com.tayota.userservice.mapper.UserMapper;
import com.tayota.userservice.model.UserSession;
import com.tayota.userservice.repository.UserRepository;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.util.CookieUtil;
import com.tayota.userservice.util.IpUtil;
import com.tayota.userservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenService verificationTokenService;
    private final NotificationGrpcClient notificationGrpcClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${api-gate-port:8090}")
    private String apiGatePort;

    // Thời gian hết hạn của refresh-token
    @Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpirationMs;

    // Khởi tạo AuthenticationManager để xác thực người dùng
    private final AuthenticationManager authenticationManager;

    // Khởi tạo JwtUtil để tạo token
    private final JwtUtil jwtUtil;

    // Khởi tạo CookieUtil để quản lý cookie
    private final CookieUtil cookieUtil;


    // Đăng ký tài khoản người dùng
    public void register(RegisterRequestDTO registerRequestDTO) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(registerRequestDTO.getEmail())) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        try {
            // Băm password
            String newPasswordHash = passwordEncoder.encode(registerRequestDTO.getPassword());

            // Tạo user với status UNVERIFIED
            User user = UserMapper.toEntity(registerRequestDTO, newPasswordHash);
            user.setStatus(StatusType.UNVERIFIED);

            // Lưu user vào database
            User savedUser = userRepository.save(user);

            // Tạo verification token
            String token = verificationTokenService.generateAndSaveToken(savedUser.getEmail());

            // Tạo verification link
            String verificationLink = frontendUrl + apiGatePort + "/verify?email=" + savedUser.getEmail() + "&token=" + token;

            // Gửi email xác thực qua gRPC (bất đồng bộ - không chặn quá trình đăng ký)
            notificationGrpcClient.sendVerificationEmail(savedUser.getEmail(), verificationLink);
        }
        catch (Exception e) {
            throw new CustomException(500, "Registration failed: " + e.getMessage());
        }
    }

    // Xác thực tài khoản qua email và token
    public void verify(String email, String token) {
        // Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra trạng thái người dùng
        if (user.getStatus() == StatusType.ACTIVE) {
            throw new CustomException(403, "Tài khoản đã được xác thực!");
        }

        // Kiểm tra token hợp lệ
        if (!verificationTokenService.verifyToken(email, token)) {
            throw new CustomException(400, "Mã xác thực không hợp lệ!");
        }

        // Cập nhật status thành ACTIVE
        user.setStatus(StatusType.ACTIVE);
        userRepository.save(user);

        // Xoá token khỏi Redis
        verificationTokenService.deleteToken(email);

        // Gửi thông báo xác thực thành công
        notificationGrpcClient.sendRegistrationSuccessEmail(email);
    }

    // Đăng nhập tài khoản
    public void login(LoginRequestDTO loginRequestDTO, HttpServletRequest request, HttpServletResponse response) {
        /* Bước 1: Kiểm tra số lần đăng nhập thất bại để tránh spam trên 1 IP */
        // Lấy thông tin IP người dùng để ngăn spam đăng nhập
        String clientIp = IpUtil.getClientIp(request);

        // Tạo key cho mỗi email để đếm số lần đăng nhập thất bại
        String redisKey = "auth:login:failed:" + loginRequestDTO.getEmail() + ":" + clientIp;
        // Truy xuất số lần nhập sai
        Integer failedCountStr = (Integer) redisTemplate.opsForValue().get(redisKey);

        // Kiểm tra số lần nhập sai vượt quá giới hạn
        if (failedCountStr != null && failedCountStr >= 5) {
            throw new CustomException(403, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau!");
        }

        try {
            /* Bước 2: Thực hiện đăng nhập từ Spring Security */
            // Gửi username/password vào DaoAuthenticationProvider
            // Provider gọi loadUserByUsername() trong CustomUserDetailsService
            // Kiểm tra mật khẩu và trạng thái người dùng
            // So sánh password bằng PasswordEncoder
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword())
            );

            /* Bước 3: Xoá số lần nhập sai mật khẩu */
            redisTemplate.delete(redisKey);

            /* Bước 4: Lấy thông tin user sau khi đăng nhập thành công */
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            String userId = userDetails.getId().toString();
            List<String> roles = userDetails.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            /*
            * Bước 5: Trường hợp tồn tại refresh-token cũ trong Cookie người dùng khi đăng nhập trước đó
            * Xoá phiên cũ của user đó trên thiết bị hiện tại (refresh-token, deviceId) để đảm bảo mỗi thiết bị chỉ có 1 refresh-token hợp lệ
            */

            // Lấy refresh-token cũ từ Cookie
            String oldRefreshToken = cookieUtil.getCookieValue(request, "refresh_token");
            log.info(">>> [LOGIN] oldRefreshToken from cookie: {}",
                    oldRefreshToken != null ? "oldRefreshToken" : "NULL");

            if (oldRefreshToken != null) {
                try {
                    // Xác thực và truy xuất deviceId từ refresh-token cũ
                    String oldDeviceId = jwtUtil.validateAndExtractDeviceIdFromRefreshToken(oldRefreshToken);

                    // Tạo session-key cũ
                    String oldSessionKey = "auth:refresh:" + userId + ":" + oldDeviceId;

                    if (jwtUtil.compareToRefreshTokenHash(oldRefreshToken, oldSessionKey)) {
                        // Kiểm tra refresh-token cũ hợp lệ hay không so với refresh-token được lưu trong Redis và xoá nó
                        // Xoá sessionKey cũ trong Redis
                        redisTemplate.delete(oldSessionKey);
                        // Xoá deviceId cũ khỏi danh sách userSessionsKey trong Redis
                        String userSessionsKey = "auth:user_sessions:" + userId;
                        redisTemplate.opsForSet().remove(userSessionsKey, oldDeviceId);
                    }
                }
                catch (Exception e) {
                    log.warn("Old refresh token is invalid or expired: {}", e.getMessage());
                }
            }

            /* Bước 6: Tạo ra deviceId để định danh thiết bị người dùng */
            // Tạo deviceId mới từ UUID
            String deviceId = UUID.randomUUID().toString();

            /* Bước 7: Kiểm tra giới hạn số thiết bị được đăng nhập trên 1 tài khoản */
            // Tạo userSessionKey các deviceId vào danh sách user đã đăng nhập
            String userSessionsKey = "auth:user_sessions:" + userId;

            // Lấy danh sách thiết bị đã đăng nhập của user đó từ Redis
            Set<Object> devices = redisTemplate.opsForSet().members(userSessionsKey);

            // Kiểm tra nếu số thiết bị đã đăng nhập vượt quá 5 thì từ chối đăng nhập trên thiết bị mới
            // !!! Khi truy cập đồng thời có thể vượt qua điều kiện, nên dùng Lua script hoặc Redis transaction
            if (devices != null && devices.size() >= 5) {
                throw new CustomException(403, "Bạn đã đăng nhập quá nhiều thiết bị");
            }

            // Lưu thiết bị đăng nhập mới vào danh sách thiết bị đăng nhập trong Redis
            redisTemplate.opsForSet().add(userSessionsKey, deviceId);
            redisTemplate.expire(userSessionsKey, Duration.ofMillis(jwtRefreshTokenExpirationMs));

            /* Bước 8: Tạo ra access-token và refresh-token */
            // Lấy thông tin user-agent từ header
            String userAgent = request.getHeader("User-Agent");
            // Thực hiện tạo cặp token
            TokenPair tokenPair = jwtUtil.generateTokenPair(userId, roles, deviceId, userAgent);

            /* Bước 9: Tạo phiên mới để phân biệt thiệt bị user trong Redis phục vụ cho chức năng logout, logout-all, revoke */
            // Tạo sessionKey
            String sessionKey = "auth:refresh:" + userId + ":" + deviceId;

            // Băm refresh token trước khi lưu vào Redis để bảo mật
            String refreshHash = jwtUtil.hashRefreshToken(tokenPair.getRefreshToken());

            // Tạo sessionValue
            UserSession sessionValue = new UserSession(refreshHash, clientIp, userAgent, Instant.now());

            // Lưu session của user vào Redis
            redisTemplate.opsForValue().set(sessionKey, sessionValue, Duration.ofMillis(jwtRefreshTokenExpirationMs));

            /* Bước 10: Gắn cặp token và lưu vào HttptOnly Cookie */
            cookieUtil.setTokenCookies(response, tokenPair.getAccessToken(), tokenPair.getRefreshToken());
        }
        // Bắt lỗi khi sai mật khẩu
        catch (BadCredentialsException e) {
            // Tăng số lần nhập sai
            Integer failedCount = failedCountStr == null ? 1 : failedCountStr + 1;

            // Lưu số lần nhập sai email theo IP vào Redis
            redisTemplate.opsForValue().set(redisKey, failedCount, Duration.ofMinutes(5));

            // Ném lỗi cả 2 email hoặc mật khẩu vì bảo mật
            throw new CustomException(401, "Email hoặc mật khẩu không đúng!");
        }
    }
}
