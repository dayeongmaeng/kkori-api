package com.kkori.api.admin.service;

import com.kkori.api.admin.dto.request.AdminDashboardPeriodRequest;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardMembersResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardOverviewResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardPetsResponse;
import com.kkori.api.log.repository.DailyLogRepository;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.entity.Species;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.photo.repository.DailyPhotoRepository;
import com.kkori.api.user.entity.OAuthProvider;
import com.kkori.api.user.entity.User;
import com.kkori.api.user.entity.UserStatus;
import com.kkori.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private DailyLogRepository dailyLogRepository;
    @Mock
    private DailyPhotoRepository dailyPhotoRepository;

    private AdminDashboardService service;
    private final AdminDashboardPeriodRequest period =
            AdminDashboardPeriodRequest.of("2026-03-01", "2026-03-07", "DAY");

    @BeforeEach
    void setUp() {
        service = new AdminDashboardService(userRepository, petRepository, dailyLogRepository, dailyPhotoRepository);
    }

    @Test
    void overviewTotalMembersCountsOnlyActiveUsers() {
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(42L);

        AdminDashboardOverviewResponse response = service.getOverview(period);

        assertThat(response.totalMembers()).isEqualTo(42L);
    }

    @Test
    void membersByProviderExcludesWithdrawnMembersWhoseProviderWasAnonymized() {
        User activeGoogleUser = user(1L, OAuthProvider.GOOGLE, UserStatus.ACTIVE);
        User withdrawnUser = withdrawnUser(2L); // provider=null (탈퇴 시 익명화됨)
        when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of(activeGoogleUser));
        when(userRepository.countByStatus(any())).thenReturn(0L);

        AdminDashboardMembersResponse response = service.getMembers(period);

        assertThat(response.byProvider().get("GOOGLE")).isEqualTo(1L);
        assertThat(response.byProvider().get("KAKAO")).isEqualTo(0L);
    }

    @Test
    void membersTotalsUseStatusCountsDirectly() {
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(10L);
        when(userRepository.countByStatus(UserStatus.WITHDRAWN)).thenReturn(3L);

        AdminDashboardMembersResponse response = service.getMembers(period);

        assertThat(response.totalMembers()).isEqualTo(10L);
        assertThat(response.withdrawnMembers()).isEqualTo(3L);
    }

    @Test
    void petsBySpeciesExcludesPetDeletedBeforeToDate() {
        Pet stillAlive = pet(1L, Species.DOG, LocalDateTime.of(2026, 2, 1, 0, 0), null);
        Pet deletedBeforeTo = pet(2L, Species.CAT,
                LocalDateTime.of(2026, 2, 1, 0, 0), LocalDateTime.of(2026, 3, 2, 0, 0)); // to(3/7)보다 훨씬 전에 삭제
        when(petRepository.findByCreatedAtLessThan(any())).thenReturn(List.of(stillAlive, deletedBeforeTo));
        when(userRepository.findByStatus(UserStatus.ACTIVE)).thenReturn(List.of());

        AdminDashboardPetsResponse response = service.getPets(period);

        assertThat(response.bySpecies().get("DOG")).isEqualTo(1L);
        assertThat(response.bySpecies().get("CAT")).isEqualTo(0L);
        assertThat(response.totalPets()).isEqualTo(1L);
    }

    @Test
    void petsPerMemberDistributionCountsActiveMembersWithNoPetsAsZeroBucket() {
        User activeMemberWithoutPets = user(1L, OAuthProvider.GOOGLE, UserStatus.ACTIVE);
        when(petRepository.findByCreatedAtLessThan(any())).thenReturn(List.of());
        when(userRepository.findByStatus(UserStatus.ACTIVE)).thenReturn(List.of(activeMemberWithoutPets));

        AdminDashboardPetsResponse response = service.getPets(period);

        assertThat(response.petsPerMemberDistribution().get("0")).isEqualTo(1L);
    }

    private User user(Long id, OAuthProvider provider, UserStatus status) {
        User user = User.builder()
                .externalId("member-" + id)
                .provider(provider)
                .providerUserId(provider.name() + "-" + id)
                .email("user" + id + "@example.com")
                .nickname("nick" + id)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "status", status);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 2, 1, 0, 0));
        return user;
    }

    private User withdrawnUser(Long id) {
        User user = User.builder().externalId("member-" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "status", UserStatus.WITHDRAWN);
        ReflectionTestUtils.setField(user, "provider", null);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 2, 1, 0, 0));
        ReflectionTestUtils.setField(user, "deletedAt", LocalDateTime.of(2026, 2, 15, 0, 0));
        return user;
    }

    private Pet pet(Long id, Species species, LocalDateTime createdAt, LocalDateTime deletedAt) {
        Pet pet = Pet.builder()
                .externalId("pet-" + id)
                .userId(null)
                .deviceId(100L)
                .name("pet" + id)
                .species(species)
                .neutered(false)
                .build();
        ReflectionTestUtils.setField(pet, "id", id);
        ReflectionTestUtils.setField(pet, "createdAt", createdAt);
        ReflectionTestUtils.setField(pet, "deletedAt", deletedAt);
        return pet;
    }
}
