package com.tayota.userservice.service;

import com.tayota.commoncore.dto.ErrorCode;
import com.tayota.commoncore.util.SecurityUtil;
import com.tayota.userservice.dto.Request.LoginRequestDTO;
import com.tayota.userservice.dto.Response.AccessTokenResponseDTO;
import com.tayota.userservice.dto.Response.DeviceResponseDTO;
import com.tayota.userservice.object.RegisterCacheData;
import com.tayota.userservice.object.TokenPair;
import com.tayota.userservice.object.CustomUserDetails;
import com.tayota.userservice.grpc.NotificationGrpcClient;
import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.mapper.UserMapper;
import com.tayota.userservice.repository.UserRepository;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.util.CookieUtil;
import com.tayota.userservice.util.IpUtil;
import com.tayota.userservice.util.JwtUtil;
import com.tayota.userservice.util.SessionUtil;
import io.jsonwebtoken.Claims;
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
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tayota.commoncore.util.SecurityUtil.ROLE_HIERARCHY_MAP;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final NotificationGrpcClient notificationGrpcClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SessionUtil sessionUtil;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${api-gate-port:8090}")
    private String apiGatePort;

    private static final String VERIFICATION_TOKEN_KEY_PREFIX = "auth:verification_token:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String USER_SESSIONS_KEY_PREFIX = "auth:user_sessions:";

    // Đăng ký tài khoản người dùng
    public void register(RegisterRequestDTO registerRequestDTO) {
        String email = registerRequestDTO.getEmail();

        // Kiểm tra email đã tồn tại trong database
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        // Kiêm tra nếu đã tồn tại token đăng ký chưa xác thực trong Redis để tránh spam đăng ký nhiều lần trên cùng 1 email
        if (redisTemplate.opsForValue().get(VERIFICATION_TOKEN_KEY_PREFIX + email) != null) {
            throw new CustomException(400, "Bạn đã đăng ký tài khoản. Vui lòng kiểm tra email để xác thực tài khoản!");
        }

        try {
            // Băm password bằng Bcrypt
            String newPasswordHash = passwordEncoder.encode(registerRequestDTO.getPassword());

            // Tạo user mới nhưng không lưu vào database
            User pendingUser = UserMapper.toEntity(registerRequestDTO, newPasswordHash);

            // Tạo token ngẫu nhiên
            String token = UUID.randomUUID().toString();

            // Tạo key cho token trong Redis
            String key = VERIFICATION_TOKEN_KEY_PREFIX + email;

            // Tạo Object chứa thông tin user và token để lưu vào Redis
             RegisterCacheData registeredUser = new RegisterCacheData(pendingUser, token);

            // Lưu token vào Redis với thời gian hết hạn 15 phút
            redisTemplate.opsForValue().set(key, registeredUser, 1, TimeUnit.MINUTES);

            // Tạo link xác thực
            String verificationLink = String.format("%s/verify?email=%s&token=%s", frontendUrl, email, token);

            // Gửi email xác thực qua gRPC
            notificationGrpcClient.sendVerificationEmail(email, verificationLink);
        }
        catch (Exception e) {
            throw new CustomException(500, "Đăng ký thất bại: " + e.getMessage());
        }
    }

    // Xác thực tài khoản qua email và token
    public void verify(String email, String token) {
        // Tạo key cho token trong Redis
        String key = VERIFICATION_TOKEN_KEY_PREFIX + email;

        // Lấy dữ liệu người dùng đăng ký từ Redis
        Object registeredUserObject = redisTemplate.opsForValue().get(key);

        // Kiểm tra nếu không tồn tại token trong Redis hoặc đã hết hạn
        if (registeredUserObject == null) {
            throw new CustomException(401, "Link xác thực không hợp lệ hoặc đã hết hạn. Vui lòng đăng ký lại tài khoản!");
        }

        // Chuyển đổi Object lấy từ Redis về đúng kiểu dữ liệu RegisterCacheData
        RegisterCacheData registeredUser = objectMapper.convertValue(registeredUserObject, RegisterCacheData.class);

        // Kiểm tra token truyền vào có khớp với token đã lưu trong Redis
        String storedToken = registeredUser.getToken();
        if (!storedToken.equals(token)) {
            throw new CustomException(401, "Link xác thực không hợp lệ!");
        }

        // Lưu tài khoản người dùng vào database
        User user = registeredUser.getUser();
        userRepository.save(user);

        // Xoá token khỏi Redis
        redisTemplate.delete(key);

        // Gửi thông báo xác thực thành công
        notificationGrpcClient.sendRegistrationSuccessEmail(user.getEmail());
    }

    // Đăng nhập tài khoản
    public AccessTokenResponseDTO login(LoginRequestDTO loginRequestDTO, HttpServletRequest request, HttpServletResponse response) {
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

            /* Bước 4: Lấy thông tin user sau khi đăng nhập thành công từ Spring Security */
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

            if (oldRefreshToken != null) {
                // Xác thực và truy xuất deviceId từ refresh-token cũ
                String oldDeviceId = jwtUtil.getClaims(oldRefreshToken).get("deviceId", String.class);

                // Kiểm tra: nếu hash trong Redis khớp với refresh token cũ thì xóa session cũ
                String oldSessionKey = REFRESH_TOKEN_KEY_PREFIX + userId + ":" + oldDeviceId;

                if (jwtUtil.compareToRefreshTokenHash(oldRefreshToken, oldSessionKey)) {
                    // Xóa: dùng RedisUtil xóa session và remove deviceId khỏi set
                    sessionUtil.deleteSession(userId, oldDeviceId);
                }
            }

            /* Bước 6: Tạo ra deviceId để định danh thiết bị người dùng */
            // Tạo deviceId mới từ UUID
            String deviceId = UUID.randomUUID().toString();

            /* Bước 7: Kiểm tra giới hạn số thiết bị được đăng nhập trên 1 tài khoản */
            // Tạo userSessionKey các deviceId vào danh sách user đã đăng nhập
            String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;

            // Lấy số lượng thiết bị đã đăng nhập và xoá các lần đăng nhập hết hạn của user đó từ Redis
            int deviceCount = sessionUtil.countActiveDevices(userId);

            // Kiểm tra nếu số thiết bị đã đăng nhập vượt quá 5 thì từ chối đăng nhập trên thiết bị mới
            // !!! Khi truy cập đồng thời có thể vượt qua điều kiện, nên dùng Lua script hoặc Redis transaction
            if (deviceCount >= 5) {
                throw new CustomException(403, "Bạn đã đăng nhập quá nhiều thiết bị");
            }

            /* Bước 8: Tạo ra access-token và refresh-token */
            // Lấy thông tin user-agent từ header
            String userAgent = request.getHeader("User-Agent");
            // Thực hiện tạo cặp token
            TokenPair tokenPair = jwtUtil.generateTokenPair(userId, roles, deviceId, userAgent);

            /* Bước 9: Lưu session mới và thêm device vào user_sessions */
            sessionUtil.saveSession(userId, deviceId, tokenPair.getRefreshToken(), clientIp, userAgent);

            /* Bước 10: Chỉ lưu refresh-token vào HttpOnly Cookie, access-token trả về body để client gửi qua Authorization header */
            cookieUtil.setRefreshTokenCookie(response, tokenPair.getRefreshToken());
            return new AccessTokenResponseDTO(tokenPair.getAccessToken());
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

    // Làm mới access-token bằng refresh-token
    public AccessTokenResponseDTO refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Lấy refresh-token từ Cookie
        String refreshToken = cookieUtil.getCookieValue(request, "refresh_token");

        if (refreshToken == null) {
            throw new CustomException(401, "Refresh token không tồn tại!");
        }

        // Xác thực và truy xuất thông tin từ refresh-token
        Claims claims = jwtUtil.getClaims(refreshToken);
        String userId = claims.getSubject();
        String deviceId = claims.get("deviceId", String.class);

        // Tạo sessionKey
        String sessionKey = REFRESH_TOKEN_KEY_PREFIX + userId + ":" + deviceId;

        // Kiểm tra refresh-token hợp lệ hay không so với refresh-token được lưu trong Redis
        if (!jwtUtil.compareToRefreshTokenHash(refreshToken, sessionKey)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Truy xuất thông tin từ refresh-token
        String userAgent = request.getHeader("User-Agent");
        List<String> roles = claims.get("role", List.class);

        // Tạo cặp token mới
        TokenPair tokenPair = jwtUtil.generateTokenPair(userId, roles, deviceId, userAgent);

        // Lưu session mới (hash refresh-token) và cập nhật expiry
        sessionUtil.saveSession(userId, deviceId, tokenPair.getRefreshToken(), IpUtil.getClientIp(request), userAgent);

        // Cập nhật refresh-token trong HttpOnly Cookie, access-token mới trả về body
        cookieUtil.setRefreshTokenCookie(response, tokenPair.getRefreshToken());
        return new AccessTokenResponseDTO(tokenPair.getAccessToken());
    }

    // Đăng xuất tài khoản
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // Lấy refresh-token từ Cookie
        String refreshToken = cookieUtil.getCookieValue(request, "refresh_token");

        if (refreshToken != null) {
            // Xác thực và truy xuất thông tin từ refresh-token
            Claims claims = jwtUtil.getClaims(refreshToken);
            String userId = claims.getSubject();
            String deviceId = claims.get("deviceId", String.class);

            // Xóa session của thiết bị người dùng
            sessionUtil.deleteSession(userId, deviceId);

            // Xóa cookie chứa token khỏi trình duyệt
            cookieUtil.clearRefreshTokenCookie(response);
        }
        else {
            throw new CustomException(401, "Refresh token không tồn tại!");
        }
    }

    // Đăng xuất tất cả tài khoản trừ thiết bị hiện tại
    public AccessTokenResponseDTO logoutAll(HttpServletRequest request, HttpServletResponse response) {
        // Lấy refresh-token từ Cookie
        String refreshToken = cookieUtil.getCookieValue(request, "refresh_token");

        if (refreshToken != null) {
            // Xác thực và truy xuất thông tin từ refresh-token
            Claims claims = jwtUtil.getClaims(refreshToken);
            String userId = claims.getSubject();
            String deviceId = claims.get("deviceId", String.class);

            // Xóa tất cả session của thiết bị người dùng
            sessionUtil.deleteAllSessions(userId);

            // Tạo lại token pair mới cho thiết bị hiện tại
            List<String> roles = claims.get("role", List.class);
            String userAgent = request.getHeader("User-Agent");
            TokenPair newTokenPair = jwtUtil.generateTokenPair(userId, roles, deviceId, userAgent);

            // Lưu thiết bị hiện tại với session mới
            sessionUtil.saveSession(userId, deviceId, newTokenPair.getRefreshToken(), IpUtil.getClientIp(request), userAgent);

            // Cập nhật refresh-token trong Cookie và trả access-token mới về body cho thiết bị hiện tại
            cookieUtil.setRefreshTokenCookie(response, newTokenPair.getRefreshToken());
            return new AccessTokenResponseDTO(newTokenPair.getAccessToken());
        }
        else {
            throw new CustomException(401, "Refresh token không tồn tại!");
        }
    }

     // Lấy danh sách thiết bị đã đăng nhập của một người dùng
     public List<DeviceResponseDTO> getDevices(String userId) {
         // Lấy userId của user hiện tại
         String currentUserId = SecurityUtil.getCurrentUserId();

         // Nếu người dùng xem thiết bị của chính mình thì cho phép trực tiếp
         if (currentUserId.equals(userId)) {
             return sessionUtil.getUserDevices(userId);
         }

         // Lấy role của user hiện tại từ SecurityContext
         String currentUserRole = SecurityUtil.getCurrentUserRole();

          // Lấy user mục tiêu từ database để kiểm tra role
          Optional<User> targetUser = userRepository.findById(UUID.fromString(userId));

         // Kiểm tra quyền user hiện tại có thể xem thiết bị của user mục tiêu không
         if (targetUser.isEmpty() || ROLE_HIERARCHY_MAP.get(currentUserRole) <= ROLE_HIERARCHY_MAP.get("ROLE_" + targetUser.get().getRole().name())) {
             throw new CustomException(403, "Bạn không có quyền xem danh sách thiết bị của người dùng này");
         }

         // Lấy danh sách thiết bị đã đăng nhập của user
         return sessionUtil.getUserDevices(userId);
     }

    // Thu hồi quyền truy cập của một thiết bị theo deviceId
    public void revokeDevice(String userId, String deviceId, HttpServletRequest request) {
        // Lấy userId của user hiện tại từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserId();

        // Nếu user đang thao tác là chủ tài khoản thì không được thu hồi thiết bị đang sử dụng hiện tại
        if (currentUserId.equals(userId)) {
            // Lấy deviceId hiện tại từ refresh_token cookie được gửi lên sau khi giải mã
            String currentRefreshToken = cookieUtil.getCookieValue(request, "refresh_token");

            if (currentRefreshToken != null) {
                // Lấy claims trực tiếp từ jwtUtil (jwtUtil sẽ ném ExpiredJwtException / JwtException nếu có vấn đề)
                Claims currentClaims = jwtUtil.getClaims(currentRefreshToken);
                String currentDeviceId = currentClaims.get("deviceId", String.class);
                if (currentDeviceId != null && currentDeviceId.equals(deviceId)) {
                    // Không cho phép thu hồi chính thiết bị đang sử dụng
                    throw new CustomException(403, "Không thể thu hồi thiết bị đang sử dụng");
                }
            }
        }

        else {
            // Lấy role của user hiện tại từ SecurityContext
            String currentUserRole = SecurityUtil.getCurrentUserRole();

            // Lấy user mục tiêu để so sánh phân cấp role
            Optional<User> targetUser = userRepository.findById(UUID.fromString(userId));

            // Kiểm tra quyền user hiện tại có thể thu hồi thiết bị của user mục tiêu không
            if (targetUser.isEmpty() || ROLE_HIERARCHY_MAP.get(currentUserRole) <= ROLE_HIERARCHY_MAP.get("ROLE_" + targetUser.get().getRole().name())) {
                throw new CustomException(403, "Bạn không có quyền thu hồi thiết bị của người dùng này");
            }
        }

        // Kiểm tra thiết bị tồn tại trong Redis
        String sessionKey = REFRESH_TOKEN_KEY_PREFIX + userId + ":" + deviceId;
        if (!redisTemplate.hasKey(sessionKey)) {
            throw new CustomException(404, "Thiết bị không được tìm thấy");
        }

        // Xoá session của deviceId
        sessionUtil.deleteSession(userId, deviceId);
    }
}
