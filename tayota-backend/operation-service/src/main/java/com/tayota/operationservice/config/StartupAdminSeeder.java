package com.tayota.operationservice.config;

import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.entity.user.UserProfile;
import com.tayota.operationservice.enums.user.ProviderType;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class StartupAdminSeeder implements ApplicationRunner {
    private static final String[] ADMIN_EMAILS = {"admin@tayota.com", "admin@tayota.vn"};
    private static final String DEMO_ADMIN_HASH = "$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${tayota.seed.admin-password:}")
    private String adminPassword;

    @Value("${tayota.seed.admin-password-hash:" + DEMO_ADMIN_HASH + "}")
    private String adminPasswordHash;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String email : ADMIN_EMAILS) {
            User admin = userRepository.findByEmail(email).orElseGet(() -> createAdmin(email));
            ensureAdminProfile(admin);
        }
    }

    private User createAdmin(String email) {
        String passwordHash = StringUtils.hasText(adminPassword)
                ? passwordEncoder.encode(adminPassword)
                : adminPasswordHash;

        User user = User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .loginProvider(ProviderType.LOCAL)
                .role(RoleType.ADMIN)
                .status(StatusType.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    private void ensureAdminProfile(User admin) {
        if (userProfileRepository.existsById(admin.getId())) {
            return;
        }

        String displayName = "Tayota Admin";
        if ("admin@tayota.vn".equalsIgnoreCase(admin.getEmail())) {
            displayName = "Tayota Admin VN";
        }

        UserProfile profile = UserProfile.builder()
                .user(admin)
                .fullname(displayName)
                .phone("0901000000")
                .gender(true)
                .birthDate(LocalDate.of(1988, 1, 1))
                .address("Tayota Head Office")
                .avatarUrl("/default-avatar.png")
                .build();

        userProfileRepository.save(profile);
    }
}
