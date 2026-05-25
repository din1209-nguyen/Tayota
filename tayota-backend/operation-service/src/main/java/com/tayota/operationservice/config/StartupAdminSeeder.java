package com.tayota.operationservice.config;

import com.tayota.operationservice.entity.car.Dealership;
import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.entity.user.UserProfile;
import com.tayota.operationservice.entity.user.ServiceAdvisor;
import com.tayota.operationservice.entity.workorder.Mechanic;
import com.tayota.operationservice.enums.user.ProviderType;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.repository.car.DealershipRepository;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.user.UserRepository;
import com.tayota.operationservice.repository.workorder.MechanicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StartupAdminSeeder implements ApplicationRunner {
    private static final UUID DEFAULT_DEALERSHIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String DEFAULT_DEALERSHIP_PLACE_ID = "tayota-district-1";
    private static final List<DemoAccount> DEMO_ACCOUNTS = List.of(
            new DemoAccount("admin.demo@tayota.com", "Tayota Demo Admin", "0903000010", RoleType.ADMIN),
            new DemoAccount("manager.demo@tayota.com", "Tayota Demo Manager", "0903000011", RoleType.MANAGER),
            new DemoAccount("advisor.demo@tayota.com", "Tayota Demo Advisor", "0903000012", RoleType.SERVICE_ADVISOR),
            new DemoAccount("assistant.demo@tayota.com", "Tayota Demo Assistant", "0903000013", RoleType.ASSISTANT),
            new DemoAccount("mechanic.demo@tayota.com", "Tayota Demo Mechanic", "0903000014", RoleType.MECHANIC),
            new DemoAccount("customer.demo@tayota.com", "Tayota Demo Customer", "0903000015", RoleType.USER)
    );
    // BCrypt hash for the documented local demo password: Tayota@123.
    private static final String DEMO_PASSWORD_HASH = "$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final DealershipRepository dealershipRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final MechanicRepository mechanicRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${tayota.seed.admin-password:}")
    private String adminPassword;

    @Value("${tayota.seed.admin-password-hash:" + DEMO_PASSWORD_HASH + "}")
    private String adminPasswordHash;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Dealership dealership = ensureDefaultDealership();

        for (DemoAccount account : DEMO_ACCOUNTS) {
            User user = userRepository.findByEmail(account.email())
                    .orElseGet(() -> createAccount(account));
            ensureProfile(user, account);
            ensureStaffAssignment(user, dealership);
        }
    }

    private User createAccount(DemoAccount account) {
        String passwordHash = StringUtils.hasText(adminPassword)
                ? passwordEncoder.encode(adminPassword)
                : adminPasswordHash;

        User user = User.builder()
                .email(account.email())
                .passwordHash(passwordHash)
                .loginProvider(ProviderType.LOCAL)
                .role(account.role())
                .status(StatusType.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    private void ensureProfile(User user, DemoAccount account) {
        if (userProfileRepository.existsById(user.getId())) {
            return;
        }

        UserProfile profile = UserProfile.builder()
                .user(user)
                .fullname(account.fullName())
                .phone(account.phone())
                .gender(true)
                .birthDate(LocalDate.of(1988, 1, 1))
                .address("Tayota Head Office")
                .avatarUrl("/default-avatar.png")
                .build();

        userProfileRepository.save(profile);
    }

    private Dealership ensureDefaultDealership() {
        return dealershipRepository.findById(DEFAULT_DEALERSHIP_ID)
                .or(() -> dealershipRepository.findByPlaceId(DEFAULT_DEALERSHIP_PLACE_ID))
                .orElseGet(() -> dealershipRepository.save(Dealership.builder()
                        .name("Tayota District 1")
                        .address("12 Le Duan, District 1, Ho Chi Minh City")
                        .latitude(new BigDecimal("10.78123456"))
                        .longitude(new BigDecimal("106.70234567"))
                        .placeId(DEFAULT_DEALERSHIP_PLACE_ID)
                        .phone("02811112222")
                        .operatingHours("08:00 - 18:00")
                        .build()));
    }

    private void ensureStaffAssignment(User user, Dealership dealership) {
        if (user.getRole() == RoleType.SERVICE_ADVISOR && !serviceAdvisorRepository.existsById(user.getId())) {
            serviceAdvisorRepository.save(ServiceAdvisor.builder()
                    .id(user.getId())
                    .dealershipId(dealership.getId())
                    .build());
        }

        if (user.getRole() == RoleType.MECHANIC && !mechanicRepository.existsById(user.getId())) {
            mechanicRepository.save(Mechanic.builder()
                    .id(user.getId())
                    .dealershipId(dealership.getId())
                    .specialty("General maintenance")
                    .averageRating(new BigDecimal("4.80"))
                    .active(true)
                    .build());
        }
    }

    private record DemoAccount(String email, String fullName, String phone, RoleType role) {
    }
}
