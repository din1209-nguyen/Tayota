package com.tayota.operationservice.service.user;

import com.tayota.operationservice.dto.request.admin.AdminUpdateDealershipRequest;
import com.tayota.operationservice.dto.response.admin.AdminUserResponse;
import com.tayota.operationservice.entity.car.Dealership;
import com.tayota.operationservice.entity.user.ServiceAdvisor;
import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.entity.user.UserProfile;
import com.tayota.operationservice.entity.workorder.Mechanic;
import com.tayota.operationservice.enums.user.ProviderType;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.repository.car.DealershipRepository;
import com.tayota.operationservice.repository.user.ServiceAdvisorRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.repository.workorder.MechanicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
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
    private DealershipRepository dealershipRepository;

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserForAdminIncludesLoginProvider() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile(userId, RoleType.USER, ProviderType.GOOGLE)));

        AdminUserResponse result = service().getUserForAdmin(userId.toString());

        assertThat(result.getLoginProvider()).isEqualTo(ProviderType.GOOGLE);
    }

    @Test
    void updateDealershipForAdminUpdatesServiceAdvisor() {
        UUID userId = UUID.randomUUID();
        UUID dealershipId = UUID.randomUUID();
        ServiceAdvisor advisor = ServiceAdvisor.builder().id(userId).dealershipId(UUID.randomUUID()).build();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile(userId, RoleType.SERVICE_ADVISOR, ProviderType.LOCAL)));
        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(activeDealership(dealershipId)));
        when(serviceAdvisorRepository.findById(userId)).thenReturn(Optional.of(advisor));

        service().updateDealershipForAdmin(userId.toString(), dealershipRequest(dealershipId));

        assertThat(advisor.getDealershipId()).isEqualTo(dealershipId);
        verify(serviceAdvisorRepository).save(advisor);
    }

    @Test
    void updateDealershipForAdminUpdatesMechanic() {
        UUID userId = UUID.randomUUID();
        UUID dealershipId = UUID.randomUUID();
        Mechanic mechanic = Mechanic.builder().id(userId).dealershipId(UUID.randomUUID()).build();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile(userId, RoleType.MECHANIC, ProviderType.LOCAL)));
        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(activeDealership(dealershipId)));
        when(mechanicRepository.findById(userId)).thenReturn(Optional.of(mechanic));

        service().updateDealershipForAdmin(userId.toString(), dealershipRequest(dealershipId));

        assertThat(mechanic.getDealershipId()).isEqualTo(dealershipId);
        verify(mechanicRepository).save(mechanic);
    }

    @Test
    void updateDealershipForAdminRejectsUnsupportedRole() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile(userId, RoleType.USER, ProviderType.LOCAL)));

        assertThatThrownBy(() -> service().updateDealershipForAdmin(userId.toString(), dealershipRequest(UUID.randomUUID())))
                .isInstanceOf(CustomException.class);

        verify(dealershipRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateDealershipForAdminRejectsInactiveDealership() {
        UUID userId = UUID.randomUUID();
        UUID dealershipId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile(userId, RoleType.MECHANIC, ProviderType.LOCAL)));
        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(
                Dealership.builder().id(dealershipId).isActive(false).build()
        ));

        assertThatThrownBy(() -> service().updateDealershipForAdmin(userId.toString(), dealershipRequest(dealershipId)))
                .isInstanceOf(CustomException.class);

        verify(mechanicRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private UserService service() {
        return new UserService(userProfileRepository, serviceAdvisorRepository, mechanicRepository, dealershipRepository);
    }

    private UserProfile profile(UUID userId, RoleType role, ProviderType providerType) {
        return UserProfile.builder()
                .id(userId)
                .fullname("Người dùng")
                .user(User.builder()
                        .id(userId)
                        .email("user@tayota.com")
                        .role(role)
                        .status(StatusType.ACTIVE)
                        .loginProvider(providerType)
                        .build())
                .build();
    }

    private AdminUpdateDealershipRequest dealershipRequest(UUID dealershipId) {
        AdminUpdateDealershipRequest request = new AdminUpdateDealershipRequest();
        request.setDealershipId(dealershipId.toString());
        return request;
    }

    private Dealership activeDealership(UUID dealershipId) {
        return Dealership.builder().id(dealershipId).isActive(true).build();
    }
}
