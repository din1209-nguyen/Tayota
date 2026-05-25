package com.tayota.operationservice.service.user;

import com.tayota.operationservice.dto.request.admin.AdminResetPasswordRequest;
import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.entity.user.UserProfile;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.workorder.MechanicRepository;
import com.tayota.operationservice.util.SessionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ServiceAdvisorRepository serviceAdvisorRepository;
    @Mock
    private MechanicRepository mechanicRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SessionUtil sessionUtil;

    @Test
    void resetPasswordByAdminUpdatesPasswordAndDeletesSessions() {
        UUID userId = UUID.randomUUID();
        UserProfile targetProfile = profile(userId);
        AdminResetPasswordRequest request = new AdminResetPasswordRequest();
        request.setPassword("Newpass1!");

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(targetProfile));
        when(passwordEncoder.encode("Newpass1!")).thenReturn("hashed-password");

        service().resetPasswordByAdmin(userId.toString(), request);

        assertThat(targetProfile.getUser().getPasswordHash()).isEqualTo("hashed-password");
        verify(sessionUtil).deleteAllSessions(userId.toString());
    }

    private UserService service() {
        return new UserService(
                userProfileRepository,
                serviceAdvisorRepository,
                mechanicRepository,
                passwordEncoder,
                sessionUtil
        );
    }

    private UserProfile profile(UUID userId) {
        return UserProfile.builder()
                .fullname("User")
                .user(User.builder()
                        .id(userId)
                        .email("user@tayota.com")
                        .role(RoleType.USER)
                        .status(StatusType.ACTIVE)
                        .build())
                .build();
    }
}
