package com.tayota.userservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tayota.commoncore.dto.ErrorCode;
import com.tayota.commoncore.util.SecurityUtil;
import com.tayota.userservice.dto.Request.GoogleLoginRequestDTO;
import com.tayota.userservice.dto.Request.LoginRequestDTO;
import com.tayota.userservice.dto.Response.AccessTokenResponseDTO;
import com.tayota.userservice.dto.Response.DeviceResponseDTO;
import com.tayota.userservice.enums.ProviderType;
import com.tayota.userservice.enums.StatusType;
import com.tayota.userservice.object.RegisterCacheData;
import com.tayota.userservice.object.TokenPair;
import com.tayota.userservice.object.CustomUserDetails;
import com.tayota.userservice.grpc.NotificationGrpcClient;
import com.tayota.userservice.dto.Request.RegisterRequestDTO;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.repository.UserRepository;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.util.CookieUtil;
import com.tayota.userservice.util.IpUtil;
import com.tayota.userservice.util.JwtUtil;
import com.tayota.userservice.util.SessionUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
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
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Arrays;
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

    // Đối tượng của Google cung cấp để xác minh tính hợp lệ của token
    private GoogleIdTokenVerifier verifier;

    // Danh sách Google Client IDs hợp lệ của ứng dụng dùng để verify ID token
    @Value("${google.client-ids}")
    private String googleClientIdsProperty;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final String VERIFICATION_TOKEN_KEY_PREFIX = "auth:verification_token:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";

    @PostConstruct
    public void init() {
        // Lấy danh sách các Client IDs từ file cấu hình (có thể có nhiều ID, phân tách bằng dấu phẩy)
        List<String> clientIds = Arrays.asList(googleClientIdsProperty.split(","));

        try {
            // Khởi tạo bộ xác thực GoogleIdTokenVerifier để sử dụng xác minh token Google
            verifier = new GoogleIdTokenVerifier.Builder(
                    // Sử dụng transport HTTP an toàn chuẩn của Google
                    GoogleNetHttpTransport.newTrustedTransport(),
                    // Sử dụng Gson để parse JSON
                    GsonFactory.getDefaultInstance()
            )
                    // Thiết lập Audience (Chỉ chấp nhận token được phát hành cho những Client ID này)
                    .setAudience(clientIds)
                    // Hoàn tất việc build đối tượng
                    .build();
        }
        catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Không thể khởi tạo Google ID token verifier", e);
        }
    }

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
            User pendingUser = User.createLocalUser(email, newPasswordHash);

            // Tạo token ngẫu nhiên
            String token = UUID.randomUUID().toString();

            // Tạo key cho token trong Redis
            String key = VERIFICATION_TOKEN_KEY_PREFIX + email;

            // Tạo Object chứa thông tin user và token để lưu vào Redis
             RegisterCacheData registeredUser = new RegisterCacheData(pendingUser, token);

            // Lưu token vào Redis với thời gian hết hạn 1 tiếng
            redisTemplate.opsForValue().set(key, registeredUser, 1, TimeUnit.HOURS);

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
        if (!StringUtils.hasText(email) || !StringUtils.hasText(token)) {
            throw new CustomException(400, "Link xác thực không hợp lệ!");
        }
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

            // Tạo session đăng nhập mới và trả về access-token cho client
            return createLoginSession(userId, roles, request, response);
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

    // Đăng nhập tài khoản bằng Google
    public AccessTokenResponseDTO loginWithGoogle(GoogleLoginRequestDTO requestDTO, HttpServletRequest request, HttpServletResponse response) {
        try {
            /* Bước 1: Xác thực token và lấy payload chứa thông tin người dùng */
            GoogleIdToken verifiedToken = verifier.verify(requestDTO.getToken());

            // Nếu token null (ví dụ: chữ ký sai, hết hạn, hoặc Client ID không khớp)
            if (verifiedToken == null) {
                throw new CustomException(401, "Google ID token không hợp lệ!");
            }

            // Lấy payload chứa thông tin người dùng
            GoogleIdToken.Payload payload = verifiedToken.getPayload();
            // Lấy ID định danh duy nhất của người dùng trên hệ thống Google
            String providerUserId = payload.getSubject();
            // Lấy địa chỉ email của người dùng
            String email = payload.getEmail();
            // Lấy thông tin xem email đã được Google xác minh chưa
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            // Lấy tên đầy đủ của người dùng
            String fullName = (String) payload.get("name");

            // Kiểm tra ID và Email phải có dữ liệu
            if (!StringUtils.hasText(providerUserId) || !StringUtils.hasText(email)) {
                throw new CustomException(401, "Google ID token thiếu thông tin định danh!");
            }

            // Kiểm tra tính tin cậy của email đảm bảo email đã được xác minh
            if (!emailVerified) {
                throw new CustomException(401, "Email chưa được xác thực bởi Google!");
            }

            User user = null;

            /* Bước 2: Truy xuất và kiểm tra user có tồn tại với providerUserId */
            // Truy xuất bằng providerUserId thay vì email sẽ đảm bảo duy nhất vì email có thể tái sử dụng lại cho người sauví dụ email công ty, học sinh,... dùng cho 1 người
            // sau đó người đó nghỉ và tạo lại email đó cho người sau (Google sẽ tạo lại email nhưng khác provideId */
            Optional<User> existedUserByProvider = userRepository.findByLoginProviderAndProviderUserId(ProviderType.GOOGLE, providerUserId);

            // Nếu chưa tồn tại user với providerUserId
            if (existedUserByProvider.isEmpty()) {
                /* Bước 3: Truy xuất và kiểm tra user có tồn tại với email */
                Optional<User> existedUserByEmail = userRepository.findByEmail(email);

                // Nếu đã tồn tại user với email này, xử lý theo luồng đặc biệt để tránh tạo trùng lặp tài khoản
                if (existedUserByEmail.isPresent()) {
                    user = existedUserByEmail.get();

                    // Trường hợp 1: Tài khoản đã đăng nhập bằng hệ thống truyền thống, nghĩa là email trùng với email Google đang đăng nhập
                    if (user.getLoginProvider() == ProviderType.LOCAL) {
                        throw new CustomException(409, "Tài khoản đã tồn tại. Vui lòng đăng nhập bằng mật khẩu!");
                    }

                    // Trường hợp 2: Email đã được dùng trước đó để đăng nhập bằng Google nhưng bị xoá và tạo lại
                    // email đó đã tồn tại nhưng providerUserId hiện tại khác với providerUserId trước đó) (giải thích chi tiết ở Bước 2)
                    if (user.getProviderUserId() != null && !user.getProviderUserId().equals(providerUserId)) {
                        throw new CustomException(409, "Email này đã liên kết với một tài khoản Google khác!");
                    }
                }
                // Nếu chưa tồn tại user nào với email này thì tạo tài khoản mới
                else {
                    user = userRepository.save(User.createGoogleUser(email, providerUserId));
                }
            }
            // Nếu đã tồn tại user với providerUserId này => người dùng đã đăng nhập bằng Google trước đó thì cho đăng nhập bình thường
            else {
                user = existedUserByProvider.get();

                // Kiểm tra trạng thái người dùng có bị ban không
                if (user.getStatus() == StatusType.BANNED) {
                    throw new CustomException(403, "Tài khoản này đã bị khóa. Vui lòng liên hệ hỗ trợ!");
                }
            }
            // Tạo session đăng nhập mới và trả về access-token cho client
            return createLoginSession(user.getId().toString(), List.of("ROLE_" + user.getRole().name()), request, response);
        }
        // Lỗi mạng (không thể kết nối tới server Google để lấy Public Key), lỗi đọc dữ liệu, lỗi về thuật toán mã hóa (máy chủ của bạn thiếu thư viện bảo mật, lỗi cấu hình JRE...)
        catch (GeneralSecurityException | IOException e) {
            throw new CustomException(401, "Google ID token không hợp lệ!");
        }
    }

    // Tạo session đăng nhập mới sau khi xác thực thành công
    public AccessTokenResponseDTO createLoginSession(String userId, List<String> roles, HttpServletRequest request, HttpServletResponse response) {

         /* Bước 5: Trường hợp tồn tại refresh-token cũ trong Cookie người dùng khi đăng nhập trước đó
         * Xoá phiên cũ của user đó trên thiết bị hiện tại (refresh-token, deviceId) để đảm bảo mỗi thiết bị chỉ có 1 refresh-token hợp lệ */
        // Lấy refresh-token cũ từ Cookie
        String oldRefreshToken = cookieUtil.getCookieValue(request, "refresh_token");

        // Nếu tồn tại refresh-token cũ (đã đăng nhập trước đó)
        if (StringUtils.hasText(oldRefreshToken)) {
            // Xác thực và truy xuất deviceId từ refresh-token cũ
            String oldDeviceId = jwtUtil.getClaims(oldRefreshToken).get("deviceId", String.class);

            // Tạo sessionKey để truy xuất hash refresh-token cũ trong Redis
            String oldSessionKey = REFRESH_TOKEN_KEY_PREFIX + userId + ":" + oldDeviceId;

            // Kiểm tra nếu hash trong Redis khớp với refresh token cũ thì xóa session cũ
            if (jwtUtil.compareToRefreshTokenHash(oldRefreshToken, oldSessionKey)) {
                sessionUtil.deleteSession(userId, oldDeviceId);
            }
        }

        /* Bước 6: Kiểm tra giới hạn số thiết bị được đăng nhập trên 1 tài khoản */
        // Lấy số lượng thiết bị đã đăng nhập và xoá các lần đăng nhập hết hạn của user đó từ Redis
        // Kiểm tra nếu số thiết bị đã đăng nhập vượt quá 5 thì từ chối đăng nhập trên thiết bị mới
        // !!! Khi truy cập đồng thời có thể vượt qua điều kiện, nên dùng Lua script hoặc Redis transaction
        if (sessionUtil.countActiveDevices(userId) >= 5) {
            throw new CustomException(403, "Bạn đã đăng nhập quá nhiều thiết bị");
        }

        /* Bước 7: Tạo ra access-token và refresh-token mới */
        // Tạo thông tin người dùng để lưu vào claim của token
        String deviceId = UUID.randomUUID().toString();
        String userAgent = request.getHeader("User-Agent");
        String clientIp = IpUtil.getClientIp(request);

        // Tạo cặp token từ các thông tin trên
        TokenPair tokenPair = jwtUtil.generateTokenPair(userId, roles, deviceId, userAgent);

        /* Bước 8: Lưu session mới và thêm device vào danh sách user_sessions  trong Redis*/
        sessionUtil.saveSession(userId, deviceId, tokenPair.getRefreshToken(), clientIp, userAgent);

        /* Bước 9: Chỉ lưu refresh-token vào HttpOnly Cookie, access-token trả về body để client gửi qua Authorization header */
        cookieUtil.setRefreshTokenCookie(response, tokenPair.getRefreshToken());
        return new AccessTokenResponseDTO(tokenPair.getAccessToken());
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
