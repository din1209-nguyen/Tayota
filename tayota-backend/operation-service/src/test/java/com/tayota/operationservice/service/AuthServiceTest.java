package com.tayota.operationservice.service;

import com.tayota.operationservice.dto.request.admin.AdminResetPasswordRequest;
import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.user.UserRepository;
import com.tayota.operationservice.repository.workorder.MechanicRepository;
import com.tayota.operationservice.service.cache.SystemCacheService;
import com.tayota.operationservice.service.auth.AuthService;
import com.tayota.operationservice.service.notification.EmailService;
import com.tayota.operationservice.util.JwtUtil;
import com.tayota.operationservice.util.OtpUtil;
import com.tayota.operationservice.util.SessionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SystemCacheService systemCacheService;
    @Mock
    private SessionUtil sessionUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private OtpUtil otpUtil;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EmailService emailService;
    @Mock
    private ServiceAdvisorRepository serviceAdvisorRepository;
    @Mock
    private MechanicRepository mechanicRepository;
    @Mock
    private UserProfileRepository userProfileRepository;

    private UUID adminId;

    @BeforeEach
    void authenticateAdmin() {
        adminId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                adminId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resetPasswordByAdminUpdatesPasswordAndDeletesSessionsForLowerRole() {
        UUID userId = UUID.randomUUID();
        AdminResetPasswordRequest request = passwordRequest();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, RoleType.USER)));
        when(passwordEncoder.encode("Newpass1!")).thenReturn("hashed-password");

        service().resetPasswordByAdmin(userId.toString(), request);

        verify(userRepository).updatePasswordHashById(userId, "hashed-password");
        verify(sessionUtil).deleteAllSessions(userId.toString());
    }

    @Test
    void resetPasswordByAdminRejectsCurrentAdmin() {
        assertThatThrownBy(() -> service().resetPasswordByAdmin(adminId.toString(), passwordRequest()))
                .isInstanceOf(CustomException.class);

        verify(userRepository, never()).updatePasswordHashById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(sessionUtil, never()).deleteAllSessions(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resetPasswordByAdminRejectsPeerAdmin() {
        UUID peerAdminId = UUID.randomUUID();
        when(userRepository.findById(peerAdminId)).thenReturn(Optional.of(user(peerAdminId, RoleType.ADMIN)));

        assertThatThrownBy(() -> service().resetPasswordByAdmin(peerAdminId.toString(), passwordRequest()))
                .isInstanceOf(CustomException.class);

        verify(userRepository, never()).updatePasswordHashById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(sessionUtil, never()).deleteAllSessions(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void changeUserStatusBansLowerRoleAndDeletesSessions() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, RoleType.USER)));

        service().changeUserStatus(userId.toString(), StatusType.BANNED);

        verify(userRepository).updateStatusById(userId, StatusType.BANNED);
        verify(sessionUtil).deleteAllSessions(userId.toString());
    }

    @Test
    void revokeRejectsDeviceOfPeerAdmin() {
        UUID peerAdminId = UUID.randomUUID();
        when(userRepository.findById(peerAdminId)).thenReturn(Optional.of(user(peerAdminId, RoleType.ADMIN)));

        assertThatThrownBy(() -> service().revoke(peerAdminId.toString(), "device-1", null))
                .isInstanceOf(CustomException.class);

        verify(sessionUtil, never()).deleteSession(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private AuthService service() {
        return new AuthService(
                passwordEncoder,
                userRepository,
                systemCacheService,
                sessionUtil,
                authenticationManager,
                jwtUtil,
                otpUtil,
                objectMapper,
                emailService,
                serviceAdvisorRepository,
                mechanicRepository,
                userProfileRepository
        );
    }

    private AdminResetPasswordRequest passwordRequest() {
        AdminResetPasswordRequest request = new AdminResetPasswordRequest();
        request.setPassword("Newpass1!");
        return request;
    }

    private User user(UUID id, RoleType role) {
        return User.builder()
                .id(id)
                .email("user@tayota.com")
                .role(role)
                .build();
    }
}
