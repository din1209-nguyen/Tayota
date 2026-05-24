package com.tayota.userservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tayota.commoncore.dto.ErrorCode;
import com.tayota.commoncore.enums.RoleType;
import com.tayota.commoncore.util.OtpUtil;
import com.tayota.commoncore.util.SecurityContextUtil;
import com.tayota.userservice.dto.Request.*;
import com.tayota.userservice.dto.Response.DeviceResponseDTO;
import com.tayota.userservice.dto.Request.ForgotPasswordResetRequestDTO;
import com.tayota.userservice.entity.ServiceAdvisor;
import com.tayota.userservice.entity.UserProfile;
import com.tayota.userservice.entity.workorder.Mechanic;
import com.tayota.userservice.enums.ProviderType;
import com.tayota.userservice.enums.StatusType;
import com.tayota.userservice.object.RegisterCacheData;
import com.tayota.userservice.object.TokenPair;
import com.tayota.userservice.object.CustomUserDetails;
import com.tayota.userservice.entity.User;
import com.tayota.userservice.repository.ServiceAdvisorRepository;
import com.tayota.userservice.repository.UserProfileRepository;
import com.tayota.userservice.repository.UserRepository;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.repository.workorder.MechanicRepository;
import com.tayota.userservice.util.*;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SessionUtil sessionUtil;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final OtpUtil otpUtil;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final MechanicRepository mechanicRepository;

    // Đối tượng của Google cung cấp để xác minh tính hợp lệ của token
    private GoogleIdTokenVerifier verifier;

    // Danh sách Google Client IDs hợp lệ của ứng dụng dùng để verify ID token
    @Value("${google.client-ids}")
    private String googleClientIdsProperty;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // Các key Redis
    private static final String VERIFICATION_TOKEN_KEY = "auth:verification_token:";
    private static final String REFRESH_TOKEN_KEY = "auth:refresh:";
    private static final String FORGOT_PASSWORD_KEY = "auth:forgot_password:";
    private static final String CHANGE_PASSWORD_KEY = "auth:change_password:";
    private final UserProfileRepository userProfileRepository;


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

    // Tạo tài khoản bởi Admin
    public void createAccount(CreateAccountRequestDTO createAccountRequestDTO) {
        // Kiểm tra email được gửi lên đã tồn tại trong csdl
        String email = createAccountRequestDTO.getEmail();
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Kiểm tra role được gửi lên có hợp lệ
        String requestedRole = createAccountRequestDTO.getRole().toUpperCase();
        RoleType roleType = null;

        try {
            // Chuyển đổi chuỗi role được gửi lên thành enum RoleType, nếu không hợp lệ sẽ ném lỗi IllegalArgumentException
            roleType = RoleType.valueOf(requestedRole);
        }
        catch (IllegalArgumentException e) {
            throw new CustomException(400, "Vai trò không hợp lệ!");
        }

        // Nếu role là SERVICE_ADVISOR thì bắt buộc phải có dealershipId, nếu không có sẽ ném lỗi
        if (roleType == RoleType.SERVICE_ADVISOR && !StringUtils.hasText(createAccountRequestDTO.getDealershipId())) {
            throw new CustomException(400, "Cố vấn dịch vụ cần thuộc một đại lý");
        }

        // Nếu role là MECHANIC thì bắt buộc phải có dealershipId, nếu không có sẽ ném lỗi
        if (roleType == RoleType.MECHANIC && !StringUtils.hasText(createAccountRequestDTO.getDealershipId())) {
            throw new CustomException(400, "Thợ kỹ thuật cần thuộc một đại lý");
        }

        // Lấy role người dùng hiện tại từ Security Context
        String currentUserRole = SecurityContextUtil.getCurrentUserRole();

        // So sánh role được yêu cầu với role của người dùng hiện tại
        if (!SecurityContextUtil.validateRoleSuperiority(currentUserRole, roleType.name())) {
            throw new CustomException(403, "Bạn không có quyền tạo tài khoản với vai trò này!");
        }

        try {
            // Băm password bằng Bcrypt trước khi lưu vào csdl
            String passwordHash = passwordEncoder.encode(createAccountRequestDTO.getPassword());

            // Tạo user mới và lưu vào database
            UserProfile user = UserProfile.builder()
                    .fullname(email.split("@")[0])
                    .user(User.builder()
                            .email(email)
                            .passwordHash(passwordHash)
                            .role(roleType)
                            .build())
                    .build();

            UserProfile savedProfile = userProfileRepository.save(user);

            // Nếu role là SERVICE_ADVISOR thì tạo thêm bản ghi trong bảng ServiceAdvisor để lưu thông tin đại lý mà Service Advisor đó thuộc về
            if (roleType == RoleType.SERVICE_ADVISOR) {
                ServiceAdvisor serviceAdvisor = ServiceAdvisor.builder()
                        .id(savedProfile.getUser().getId())
                        .dealershipId(UUID.fromString(createAccountRequestDTO.getDealershipId()))
                        .build();

                serviceAdvisorRepository.save(serviceAdvisor);
            }

            // Nếu role là MECHANIC thì tạo thêm bản ghi trong bảng Mechanic để lưu thông tin đại lý mà thợ kỹ thuật đó thuộc về và trạng thái hoạt động của thợ kỹ thuật đó
            if (roleType == RoleType.MECHANIC) {
                Mechanic mechanic = Mechanic.builder()
                        .id(savedProfile.getUser().getId())
                        .dealershipId(UUID.fromString(createAccountRequestDTO.getDealershipId()))
                        .active(true)
                        .build();

                mechanicRepository.save(mechanic);
            }

        }
        catch (Exception e) {
            throw new CustomException(500, "Tạo tài khoản thất bại: " + e.getMessage());
        }
    }

    // Đăng ký tài khoản
    public void register(RegisterRequestDTO registerRequestDTO, String clientIp) {
        /* Kiểm tra giới hạn số lần đăng ký tài khoản trên ngày với mỗi IP */
        String ipLimitKey = "auth:register:limit:ip:" + clientIp;

        Integer registerCount = (Integer) redisTemplate.opsForValue().get(ipLimitKey);

        if (registerCount != null && registerCount > 5) {
            throw new CustomException(429, "Bạn đã đăng ký quá nhiều tài khoản. Vui lòng thử lại sau!");
        }

        String email = registerRequestDTO.getEmail();

        // Kiểm tra email đã tồn tại trong csdl
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Kiêm tra nếu đã tồn tại token đăng ký chưa xác thực trong Redis để tránh spam đăng ký nhiều lần trên cùng 1 email
        if (redisTemplate.opsForValue().get(VERIFICATION_TOKEN_KEY + email) != null) {
            throw new CustomException(400, "Bạn đã đăng ký tài khoản. Vui lòng kiểm tra email để xác thực tài khoản!");
        }

        try {
            // Băm password bằng Bcrypt
            String newPasswordHash = passwordEncoder.encode(registerRequestDTO.getPassword());

            // Tạo user mới nhưng không lưu vào database
            UserProfile pendingUser = UserProfile.builder()
                    .fullname(email.split("@")[0])
                    .user(User.builder()
                            .email(email)
                            .passwordHash(newPasswordHash)
                            .build())
                    .build();

            // Tạo token ngẫu nhiên
            String token = UUID.randomUUID().toString();

            // Tạo key cho token trong Redis
            String key = VERIFICATION_TOKEN_KEY + email;

            // Tạo Object chứa thông tin user và token để lưu vào Redis
             RegisterCacheData registeredUser = new RegisterCacheData(pendingUser, token);

            // Lưu token vào Redis với thời gian hết hạn 1 tiếng
            redisTemplate.opsForValue().set(key, registeredUser, 1, TimeUnit.HOURS);

            // Tạo link xác thực
            String verificationLink = String.format("%s/verify-account?email=%s&token=%s", frontendUrl, email, token);

            // Gửi email xác thực
            emailService.sendEmailAsync(
                    email,
                    "Xác thực địa chỉ Email - Tayota",
                    "Vui lòng nhấn vào đường dẫn dưới đây để xác thực tài khoản của bạn:\n\n" +
                            verificationLink + "\n\n" +
                            "Đường dẫn này sẽ hết hạn sau 1 tiếng. Vui lòng không chia sẻ mã này với bất kỳ ai."
            );

            // Lưu số lần đăng ký lên 1 theo IP vào Redis để giới hạn
            Long newRegisterCount = redisTemplate.opsForValue().increment(ipLimitKey);

            // Chỉ set expire cho lần đầu, các lần sau increment() giữ nguyên thời gian đếm ngược
            if (newRegisterCount != null && newRegisterCount == 1) {
                redisTemplate.expire(ipLimitKey, Duration.ofHours(12));
            }
        }
        catch (Exception e) {
            throw new CustomException(500, "Đăng ký thất bại: " + e.getMessage());
        }
    }

    // Xác thực tài khoản qua email và token
    @Transactional
    public void verifyAccount(VerifyAccountRequestDTO verifyEmailRequestDTO) {
        String email = verifyEmailRequestDTO.getEmail();
        String token = verifyEmailRequestDTO.getToken();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(token)) {
            throw new CustomException(400, "Link xác thực không hợp lệ!");
        }
        // Tạo key cho token trong Redis
        String key = VERIFICATION_TOKEN_KEY + email;

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

        // Lưu tài khoản người dùng vào csld
        UserProfile userProfile = registeredUser.getUserProfile();
        userProfileRepository.save(userProfile);

        // Xoá token khỏi Redis
        redisTemplate.delete(key);

        // Gửi email thông báo xác thực thành công
        emailService.sendEmailAsync(
                email,
                "Chào mừng bạn đến với Tayota - Đăng ký thành công",
                "Tài khoản của bạn đã được khởi tạo thành công trên hệ thống Tayota. Bây giờ bạn có thể trải nghiệm các dịch vụ của chúng tôi."
        );
    }

    // Đăng nhập tài khoản
    public TokenPair login(LoginRequestDTO loginRequestDTO, String clientIp, String userAgent, String oldRefreshToken) {
        /* Bước 1: Kiểm tra số lần đăng nhập thất bại để tránh spam trên 1 IP */
        // Tạo key cho mỗi email để đếm số lần đăng nhập thất bại
        String loginLimitKey = "auth:login:limit:fail:" + loginRequestDTO.getEmail() + ":" + clientIp;
        // Truy xuất số lần nhập sai
        Integer failedCount = (Integer) redisTemplate.opsForValue().get(loginLimitKey);
        
        // Kiểm tra số lần nhập sai vượt quá giới hạn
        if (failedCount != null && failedCount > 5) {
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
            redisTemplate.delete(loginLimitKey);

            /* Bước 4: Lấy thông tin user sau khi đăng nhập thành công từ Spring Security */
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            String userId = userDetails.getId().toString();
            String email = userDetails.getEmail();
            List<String> roles = userDetails.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            // Tạo session đăng nhập mới và trả về access-token cho client
            return createLoginSession(userId, email, roles, clientIp, userAgent, oldRefreshToken);
        }
        // Bắt lỗi khi sai mật khẩu
        catch (BadCredentialsException e) {
            // Lưu số lần nhập sai email lên 1 theo IP vào Redis
            Long newFailedCount = redisTemplate.opsForValue().increment(loginLimitKey);

            // Chỉ set expire cho lần đầu, các lần sau increment() giữ nguyên thời gian đếm ngược
            if (newFailedCount != null && newFailedCount == 1) {
                redisTemplate.expire(loginLimitKey, Duration.ofHours(2));
            }

            // Ném lỗi cả 2 email hoặc mật khẩu vì bảo mật
            throw new CustomException(401, "Email hoặc mật khẩu không đúng!");
        }
    }

    // Đăng nhập tài khoản bằng Google
    @Transactional
    public TokenPair loginWithGoogle(GoogleLoginRequestDTO requestDTO, String clientIp, String userAgent, String oldRefreshToken ) {
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
            Optional<User> existingUserByProvider = userRepository.findByLoginProviderAndProviderUserId(ProviderType.GOOGLE, providerUserId);

            // Nếu chưa tồn tại user với providerUserId
            if (existingUserByProvider.isEmpty()) {
                /* Bước 3: Truy xuất và kiểm tra user có tồn tại với email */
                Optional<User> existingUserByEmail = userRepository.findByEmail(email);

                // Nếu đã tồn tại user với email này, xử lý theo luồng đặc biệt để tránh tạo trùng lặp tài khoản
                if (existingUserByEmail.isPresent()) {
                    user = existingUserByEmail.get();

                    // Trường hợp 1: Tài khoản đã đăng nhập bằng hệ thống truyền thống, nghĩa là email trùng với email Google đang đăng nhập
                    if (user.getLoginProvider() == ProviderType.LOCAL)
                    {
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
                    String fullname = payload.get("name").toString();
                    String avatarUrl = payload.get("picture").toString();
                    user = userProfileRepository.save(UserProfile.builder()
                            .fullname(fullname)
                            .avatarUrl(avatarUrl)
                            .user(User.builder()
                                    .email(email)
                                    .providerUserId(providerUserId)
                                    .loginProvider(ProviderType.GOOGLE)
                                    .build())
                            .build()).getUser();
                }
            }
            // Nếu đã tồn tại user với providerUserId này => người dùng đã đăng nhập bằng Google trước đó thì cho đăng nhập bình thường
            else {
                user = existingUserByProvider.get();

                // Kiểm tra trạng thái người dùng có bị ban không
                if (user.getStatus() == StatusType.BANNED) {
                    throw new CustomException(403, "Tài khoản này đã bị khóa. Vui lòng liên hệ hỗ trợ!");
                }
            }
            // Tạo session đăng nhập mới và trả về access-token cho client
            return createLoginSession(user.getId().toString(), user.getEmail(), List.of("ROLE_" + user.getRole().name()), clientIp, userAgent, oldRefreshToken);
        }
        // Lỗi mạng (không thể kết nối tới server Google để lấy Public Key), lỗi đọc dữ liệu, lỗi về thuật toán mã hóa (máy chủ của bạn thiếu thư viện bảo mật, lỗi cấu hình JRE...)
        catch (GeneralSecurityException | IOException e) {
            throw new CustomException(401, "Google ID token không hợp lệ!");
        }
    }

    // Tạo session đăng nhập mới sau khi xác thực thành công
    public TokenPair createLoginSession(String userId, String email, List<String> roles, String clientIp, String userAgent, String oldRefreshToken) {

         /* Bước 5: Trường hợp tồn tại refresh-token cũ trong Cookie người dùng khi đăng nhập trước đó
         * Xoá phiên cũ của user đó trên thiết bị hiện tại (refresh-token, deviceId) để đảm bảo mỗi thiết bị chỉ có 1 refresh-token hợp lệ */
        // Nếu tồn tại refresh-token cũ (đã đăng nhập trước đó)
        if (StringUtils.hasText(oldRefreshToken)) {
            // Xác thực và truy xuất deviceId từ refresh-token cũ
            String oldDeviceId = jwtUtil.getClaims(oldRefreshToken).get("deviceId", String.class);

            // Tạo sessionKey để truy xuất hash refresh-token cũ trong Redis
            String oldSessionKey = REFRESH_TOKEN_KEY + userId + ":" + oldDeviceId;

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

        // Tạo cặp token từ các thông tin trên
        TokenPair tokenPair = jwtUtil.generateTokenPair(userId, email, roles, deviceId);

        /* Bước 8: Lưu session mới và thêm device vào danh sách user_sessions trong Redis*/
        sessionUtil.saveSession(userId, deviceId, tokenPair.getRefreshToken(), clientIp, userAgent);

        // Trả cặp token về tầng Controller xử lý
        return tokenPair;
    }

    // Làm mới access-token bằng refresh-token
    public TokenPair refreshToken(String clientIp, String userAgent, String oldRefreshToken) {
        // Kiểm tra refresh-token không tồn tại thì ném lỗi
        if (!StringUtils.hasText(oldRefreshToken)) {
            throw new CustomException(401, "Refresh token không tồn tại!");
        }

        // Xác thực và truy xuất thông tin từ refresh-token
        Claims claims = jwtUtil.getClaims(oldRefreshToken);
        String email = claims.getSubject();
        String userId = claims.get("userId", String.class);
        String deviceId = claims.get("deviceId", String.class);

        // Xoá session nếu refresh-token cũ hợp lệ để đảm bảo mỗi refresh-token chỉ được sử dụng 1 lần
        sessionUtil.deleteSession(userId, deviceId);

        // Tạo sessionKey
        String sessionKey = REFRESH_TOKEN_KEY + userId + ":" + deviceId;

        // Kiểm tra refresh-token hợp lệ hay không so với refresh-token được lưu trong Redis
        if (!jwtUtil.compareToRefreshTokenHash(oldRefreshToken, sessionKey)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Truy xuất role từ refresh-token
        List<String> roles = claims.get("role", List.class);

        // Tạo cặp token mới
        TokenPair newTokenPair = jwtUtil.generateTokenPair(userId, email, roles, deviceId);

        // Lưu session mới
        sessionUtil.saveSession(userId, deviceId, newTokenPair.getRefreshToken(), clientIp, userAgent);

        // Trả cặp token về tầng Controller xử lý
        return newTokenPair;
    }

    // Đăng xuất tài khoản
    public void logout(String refreshToken) {
        // Kiểm tra refresh-token không tồn tại thì ném lỗi
        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(401, "Refresh token không tồn tại!");
        }

        // Xác thực và truy xuất thông tin từ refresh-token
        Claims claims = jwtUtil.getClaims(refreshToken);
        String userId = claims.get("userId", String.class);
        String deviceId = claims.get("deviceId", String.class);

        // Xóa session của thiết bị người dùng
        sessionUtil.deleteSession(userId, deviceId);
    }

    // Đăng xuất tất cả tài khoản trừ thiết bị hiện tại
    public TokenPair logoutAll(String clientIp, String userAgent, String refreshToken) {
        // Kiểm tra refresh-token không tồn tại thì ném lỗi
        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(401, "Refresh token không tồn tại!");
        }

        // Xác thực và truy xuất thông tin từ refresh-token
        Claims claims = jwtUtil.getClaims(refreshToken);
        String email = claims.getSubject();
        String userId = claims.get("userId", String.class);
        String deviceId = claims.get("deviceId", String.class);

        // Xóa tất cả session của thiết bị người dùng
        sessionUtil.deleteAllSessions(userId);

        // Tạo lại token pair mới cho thiết bị hiện tại
        List<String> roles = claims.get("role", List.class);
        TokenPair newTokenPair = jwtUtil.generateTokenPair(userId, email, roles, deviceId);

        // Lưu thiết bị hiện tại với session mới
        sessionUtil.saveSession(userId, deviceId, newTokenPair.getRefreshToken(), clientIp, userAgent);

        // Trả cặp token về tầng Controller xử lý
        return newTokenPair;
    }

    // Quên mật khẩu và gửi mã OTP
    public void sendForgotPasswordOTP(String email, String clientIp) {
        // Kiểm tra email đã tồn tại
        Optional<User> existingUser = userRepository.findByEmail(email);

        // Kiểm tra email không tồn tại thì không làm gì cả để tránh lộ thông tin người dùng
        if (existingUser.isEmpty()) {
            return;
        }

        // Kiểm tra tài khoản đã đăng nhập bằng Google thì không được đặt lại mật khẩu vì không có
        if (existingUser.get().getLoginProvider() != ProviderType.LOCAL) {
            throw new CustomException(400, "Tài khoản này đã đăng nhập bằng Google!");
        }

        // Kiểm tra số lần và tạo mã OTP
        String otp = otpUtil.checkAndGenerateOtp(email, clientIp, FORGOT_PASSWORD_KEY, 5, 6, Duration.ofHours(1), Duration.ofMinutes(5));

        // Gửi mã OTP đặt lại mật khẩu
        emailService.sendEmailAsync(
                email,
                "Mã OTP đặt lại mật khẩu - Tayota",
                "Mã xác thực (OTP) để đặt lại mật khẩu của bạn là: " + otp + "\n" +
                        "Mã này sẽ hết hạn sau 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai."

        );
    }

    // Xác thực mã OTP khi quên mật khẩu
    public String verifyForgotPasswordOTP(VerifyForgotPasswordOTPRequestDTO verifyForgotPasswordOTPRequestDTO, String clientIP) {
        String email = verifyForgotPasswordOTPRequestDTO.getEmail();
        String otp = verifyForgotPasswordOTPRequestDTO.getOtp();

        // Xác thực OTP, tạo token xác thực đổi mật khẩu và trả về cho người dùng
        return verifyOTPAndCreateToken(email, otp, clientIP, FORGOT_PASSWORD_KEY, 5, Duration.ofMinutes(15));
    }

    // Đặt lại mật khẩu khi xác thực OTP cho quên mật khẩu
    @Transactional
    public void resetPasswordByForgotPassword(ForgotPasswordResetRequestDTO resetPasswordRequestDTO, String clientIP) {
        String email = resetPasswordRequestDTO.getEmail();
        String token = resetPasswordRequestDTO.getToken();
        String newPassword = resetPasswordRequestDTO.getNewPassword();

        // Kiểm tra token hợp lệ trước khi đặt lại mật khẩu
        checkTokenForResetPassword(email, token, clientIP, FORGOT_PASSWORD_KEY);

        // Lấy user từ csdl
        Optional<User> existingUser = userRepository.findByEmail(email);

        // Đổi mật khẩu khi đã xác thực OTP
        resetPassword(existingUser, email, newPassword, clientIP, FORGOT_PASSWORD_KEY);
    }

    // Gửi mã OTP để xác nhận thay đổi mật khẩu
    public void sendChangePasswordOTP(String clientIp) {
        String userId = SecurityContextUtil.getCurrentUserId();

        // Lấy thông tin user từ csdl
        Optional<User>  existingUser = userRepository.findById(UUID.fromString(userId));

        if (existingUser.isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // Kiểm tra tài khoản đăng nhập bằng Google thì không được đặt lại mật khẩu vì không có
        if (existingUser.get().getLoginProvider() != ProviderType.LOCAL) {
            throw new CustomException(400, "Tài khoản này đã đăng nhập bằng Google!");
        }

        User user = existingUser.get();

        // Kiểm tra số lần và tạo mã OTP
        String otp = otpUtil.checkAndGenerateOtp(
                userId,
                clientIp,
                CHANGE_PASSWORD_KEY,
                5,
                6,
                Duration.ofHours(1),
                Duration.ofMinutes(5)
        );

        // Gửi mã OTP xác nhận thay đổi mật khẩu
        emailService.sendEmailAsync(
                user.getEmail(),
                "Mã OTP xác nhận thay đổi mật khẩu - Tayota",
                "Mã xác thực (OTP) để thay đổi mật khẩu của bạn là: " + otp + "\n" +
                        "Mã này sẽ hết hạn sau 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai."
        );
    }

    // Xác thực mã OTP khi thay đổi mật khẩu
    public String verifyChangePasswordOTP(VerifyChangePasswordOTPRequestDTO verifyChangePasswordOTPRequestDTO, String clientIP) {
        String userId = SecurityContextUtil.getCurrentUserId();
        String otp = verifyChangePasswordOTPRequestDTO.getOtp();

        // Xác thực OTP, tạo token xác thực đổi mật khẩu và trả về cho người dùng
        return verifyOTPAndCreateToken(userId, otp, clientIP, CHANGE_PASSWORD_KEY, 5, Duration.ofMinutes(15));
    }

    // Đặt lại mật khẩu khi xác thực OTP cho thay đổi mật khẩu
    @Transactional
    public void resetPasswordByChangePassword(ChangePasswordResetRequestDTO changePasswordResetRequestDTO, String clientIP) {
        String userId = SecurityContextUtil.getCurrentUserId();
        String token = changePasswordResetRequestDTO.getToken();
        String newPassword = changePasswordResetRequestDTO.getNewPassword();

        // Kiểm tra token hợp lệ trước khi đặt lại mật khẩu
        checkTokenForResetPassword(userId, token, clientIP, CHANGE_PASSWORD_KEY);

        // Lấy user từ csdl
        Optional<User> existingUser = userRepository.findById(UUID.fromString(userId));

        // Đổi mật khẩu khi đã xác thực OTP
        resetPassword(existingUser, userId, newPassword, clientIP, CHANGE_PASSWORD_KEY);
    }

    // Xác thực mã OTP và tạo token xác thực lưu vào Redis
    private String verifyOTPAndCreateToken(String subject, String otp, String clientIP, String KEY_PREFIX, int maxFailures, Duration tokenExpiry) {
        // Kiểm tra OTP hợp lệ
        otpUtil.verifyOtp(subject, otp, clientIP, KEY_PREFIX, maxFailures);

        // Tạo token để xác thực đổi mật khẩu sau khi kiểm tra OTP hợp lệ
        String token = UUID.randomUUID().toString();

        // Lưu token vào Redis để xác thực đổi mật khẩu và trả về cho người dùng
        String tokenKey = KEY_PREFIX + "reset_password:" + subject + ":" + clientIP;
        redisTemplate.opsForValue().set(tokenKey, token, tokenExpiry);
        return token;
    }

    // Kiểm tra token hợp lệ trước khi đặt lại mật khẩu
    private void checkTokenForResetPassword(String subject, String token, String clientIP, String KEY_PREFIX) {
        // Lấy token đặt lại mật khẩu đã lưu trước đó trong Redis
        String tokenKey = KEY_PREFIX + "reset_password:" + subject + ":" + clientIP;
        String storedToken = (String) redisTemplate.opsForValue().get(tokenKey);

        // Kiểm tra token hợp lệ
        if (storedToken == null || !storedToken.equals(token)) {
            throw new CustomException(400, "Token đặt lại mật khẩu không hợp lệ!");
        }
    }

    // Đổi mật khẩu khi đã xác thực OTP
    private void resetPassword(Optional<User> existingUser, String subject, String newPassword, String clientIP, String KEY_PREFIX) {
        // Kiểm tra user tồn tại và đăng nhập bằng LOCAL
        if (existingUser.isEmpty() || existingUser.get().getLoginProvider() != ProviderType.LOCAL) {
            throw new CustomException(400, "Tài khoản không hợp lệ!");
        }

        // Cập nhật mật khẩu mới sau khi băm và lưu vào csdl
        User user = existingUser.get();
        userRepository.updatePasswordHashById(user.getId(), passwordEncoder.encode(newPassword));

        // Xoá token đặt lại mật khẩu khỏi Redis
        String tokenKey = KEY_PREFIX + "reset_password:" + subject + ":" + clientIP;
        redisTemplate.delete(tokenKey);

        // Đăng xuất tất cả các thiết bị đã đăng nhập của user sau khi đổi mật khẩu để đảm bảo an toàn
        sessionUtil.deleteAllSessions(user.getId().toString());
    }

    // Lấy danh sách thiết bị đã đăng nhập của một tài khoản
    public List<DeviceResponseDTO> getDevices(String userId) {
        // Lấy userId của user hiện tại
        String currentUserId = SecurityContextUtil.getCurrentUserId();

        // Nếu người dùng xem thiết bị của chính mình thì cho phép trực tiếp
        if (currentUserId.equals(userId)) {
             return sessionUtil.getUserDevices(userId);
         }

        // Lấy role của user hiện tại từ SecurityContext
        String currentUserRole = SecurityContextUtil.getCurrentUserRole();

        // Lấy user mục tiêu từ database để kiểm tra role
        Optional<User> targetUser = userRepository.findById(UUID.fromString(userId));

        // Kiểm tra quyền user hiện tại có thể xem thiết bị của user mục tiêu không
        if (targetUser.isEmpty() || !SecurityContextUtil.validateRoleSuperiority(currentUserRole, targetUser.get().getRole().name())) {
             throw new CustomException(403, "Bạn không có quyền xem danh sách thiết bị của người dùng này");
         }

        // Lấy danh sách thiết bị đã đăng nhập của user
        return sessionUtil.getUserDevices(userId);
    }

    // Thu hồi quyền truy cập của một thiết bị
    public void revoke(String userId, String deviceId, String refreshToken) {
        // Lấy userId của user hiện tại từ SecurityContext
        String currentUserId = SecurityContextUtil.getCurrentUserId();

        // Nếu user đang thao tác là chủ tài khoản thì không được thu hồi thiết bị đang sử dụng hiện tại
        if (currentUserId.equals(userId)) {
            // Kiểm tra deviceId trong token có trùng với deviceId đang bị thu hồi
            if (refreshToken != null) {
                // Truy xuất deviceId từ refresh-token để so sánh với deviceId của thiết bị đang bị thu hồi
                Claims currentClaims = jwtUtil.getClaims(refreshToken);
                String currentDeviceId = currentClaims.get("deviceId", String.class);

                // Nếu trùng thì ném lỗi không cho phép thu hồi
                if (currentDeviceId.equals(deviceId)) {
                    // Không cho phép thu hồi chính thiết bị đang sử dụng
                    throw new CustomException(403, "Không thể thu hồi thiết bị đang sử dụng");
                }
            }
        }
        else {
            // Lấy role của user hiện tại từ SecurityContext
            String currentUserRole = SecurityContextUtil.getCurrentUserRole();

            // Lấy user mục tiêu để so sánh phân cấp role
            Optional<User> targetUser = userRepository.findById(UUID.fromString(userId));

            // Kiểm tra quyền user hiện tại có thể thu hồi thiết bị của user mục tiêu không
            if (targetUser.isEmpty() || !SecurityContextUtil.validateRoleSuperiority(currentUserRole, targetUser.get().getRole().name())) {
                throw new CustomException(403, "Không thể thực hiện thao tác trên tài khoản này.");
            }
        }

        // Kiểm tra thiết bị không tồn tại trong Redis
        String sessionKey = REFRESH_TOKEN_KEY + userId + ":" + deviceId;
        if (!redisTemplate.hasKey(sessionKey)) {
            throw new CustomException(404, "Thiết bị không được tìm thấy");
        }

        // Xoá session của deviceId
        sessionUtil.deleteSession(userId, deviceId);
    }

    // Thay đổi trạng thái người dùng
    @Transactional
    public void changeUserStatus(String userId, StatusType newStatus) {
        // Lấy userId của user hiện tại từ SecurityContext
        String currentUserId = SecurityContextUtil.getCurrentUserId();

        // Kiểm tra không được tự thao tác trên chính mình
        if (currentUserId.equals(userId)) {
            String action = (newStatus == StatusType.BANNED) ? "khoá" : "mở khoá";
            throw new CustomException(403, "Không thể " + action + " chính tài khoản của bạn!");
        }

        // Lấy role của user hiện tại từ SecurityContext
        String currentUserRole = SecurityContextUtil.getCurrentUserRole();

        // Lấy user mục tiêu để so sánh phân cấp role từ csdl
        Optional<User> targetUser = userRepository.findById(UUID.fromString(userId));

        // Kiểm tra tồn tại, trạng thái phải khác với trạng thái trước đó và phân cấp Role
        if (targetUser.isEmpty() || targetUser.get().getStatus() == newStatus || !SecurityContextUtil.validateRoleSuperiority(currentUserRole, targetUser.get().getRole().name())) {
            throw new CustomException(403, "Không thể thực hiện thao tác trên tài khoản này.");
        }

        // Cập nhật trạng thái
        userRepository.updateStatusById(UUID.fromString(userId), newStatus);

        // Nếu là BAN thì mới cần xóa session (Unban không cần xóa vì chưa có session)
        if (newStatus == StatusType.BANNED) {
            sessionUtil.deleteAllSessions(userId);
        }
    }
}
