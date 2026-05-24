package com.tayota.commoncore.util;

import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "common.otp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OtpUtil {
    private final SecureRandom secureRandom = new SecureRandom();
    private final RedisTemplate<String, Object> redisTemplate;

    // Thời gian chờ (cooldown) bắt buộc giữa 2 lần gửi OTP
    private final Duration OTP_RESEND_COOLDOWN_DURATION = Duration.ofSeconds(60);


    // Tạo mã OTP ngẫu nhiên
    public String generateOtp(int length) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(secureRandom.nextInt(10)); // Lấy số từ 0-9
        }
        return otp.toString();
    }

    // Kiểm tra số lần và tạo mã OTP theo IP
    public String checkAndGenerateOtp(String subject, String clientIp, String otpKeyPrefix, int maxRequest, int otpLength, Duration cooldownDuration, Duration otpExpiry) {
        /* Kiểm tra thời gian cooldown cho lần gửi OTP tiếp theo theo IP */
        // Key để lưu thời gian cooldown theo IP
        String resendCooldownKey = otpKeyPrefix + "cooldown:" + subject + ":" + clientIp;
        // Lấy thời gian còn lại của cooldown
        Long expireTime = redisTemplate.getExpire(resendCooldownKey);

        // Nếu còn thời gian cooldown, trả về lỗi yêu cầu người dùng đợi
        if (expireTime != null && expireTime > 0) {
            throw new CustomException(429, "Vui lòng đợi " + expireTime + " giây trước khi gửi lại OTP!");
        }

        /* Giới hạn tổng số lần gửi OTP trong một khoảng thời gian nhất định theo IP */
        // Key để lưu số lần gửi OTP theo IP
        String rateLimitKey = otpKeyPrefix + "limit:" + subject + ":" + clientIp;
        // Tăng số lần gửi OTP lên
        Long otpRequestCount = redisTemplate.opsForValue().increment(rateLimitKey);

        if (otpRequestCount != null) {
            // Lần đầu tiên gọi, set thời gian hết hạn cho key cooldown
            if (otpRequestCount == 1) {
                redisTemplate.expire(rateLimitKey, cooldownDuration);
            }
            // Kiểm tra số lần gửi OTP vượt quá giới hạn
            else if (otpRequestCount > maxRequest) {
                throw new CustomException(403, "Bạn đã gửi OTP quá nhiều lần. Vui lòng thử lại sau!");
            }
        }

        // Tạo mã OTP để xác thực đặt lại mật khẩu
        String otp = generateOtp(otpLength);

        // Lưu thời gian cooldown tiếp theo được gửi OTP vào Redis
        redisTemplate.opsForValue().set(resendCooldownKey, "cooldown", OTP_RESEND_COOLDOWN_DURATION);

        // Lưu mã OTP vào Redis
        String otpKey = otpKeyPrefix + subject + ":" + clientIp;
        redisTemplate.opsForValue().set(otpKey, otp, otpExpiry);

        // Trả mã OTP về
        return otp;
    }

    // Xác thực mã OTP theo IP
    public void verifyOtp(String subject, String otp, String clientIp, String otpKeyPrefix, int maxFailures) {
        // Key để lưu mã OTP theo IP
        String otpKey = otpKeyPrefix + subject + ":" + clientIp;

        // Lấy mã OTP được lưu trong Redis
        String storedOtp = (String) redisTemplate.opsForValue().get(otpKey);

        // Nếu mã OTP không tồn tại hoặc đã hết hạn, trả về lỗi
        if (storedOtp == null) {
            throw new CustomException(400, "Mã OTP không tồn tại hoặc đã hết hạn!");
        }

        // Key để lưu số lần nhập sai OTP theo IP
        String failKey = otpKeyPrefix + "fail:" + subject + ":" + clientIp;

        /* Kiểm tra nếu mã gửi lên không khớp với mã trong Redis */
        if (!storedOtp.equals(otp)) {
            // Tăng số lần nhập sai lên
            Long failCount = redisTemplate.opsForValue().increment(failKey);

            if (failCount != null) {
                // Lần nhập sai đầu tiên, set thời gian sống (TTL) cho failKey bằng với TTL của OTP để tự động xoá
                if (failCount == 1) {
                    Long expire = redisTemplate.getExpire(otpKey);
                    if (expire != null && expire > 0) {
                        redisTemplate.expire(failKey, Duration.ofSeconds(expire));
                    }
                }
                // Kiểm tra nếu nhập sai vượt quá số lần cho phép
                else if (failCount > maxFailures) {
                    // Xoá OTP đi để bắt buộc người dùng yêu cầu mã mới
                    redisTemplate.delete(otpKey);
                    redisTemplate.delete(failKey);
                    throw new CustomException(403, "Bạn đã nhập sai OTP quá nhiều lần. Vui lòng yêu cầu mã mới!");
                }
            }

            // Nếu mã OTP không hợp lệ, trả về lỗi
            throw new CustomException(400, "Mã OTP không hợp lệ");
        }

        // Xoá thời gian tiếp theo được gửi OTP
        redisTemplate.delete(otpKey);
        // Xoá mã OTP khỏi Redis
        redisTemplate.delete(failKey);

        // Xoá số lần gửi mã OTP theo IP
        // Có thể không cần xoá số lần gửi OTP tránh trường hợp spam tiếp
        String rateLimitKey = otpKeyPrefix + "limit:" + subject + ":" + clientIp;
        redisTemplate.delete(rateLimitKey);
    }
}
