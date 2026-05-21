package com.kkori.api.auth.service;

import com.kkori.api.auth.context.AuthContext;
import com.kkori.api.auth.context.AuthenticatedUser;
import com.kkori.api.auth.dto.request.LogoutRequest;
import com.kkori.api.auth.dto.request.OAuthLoginRequest;
import com.kkori.api.auth.dto.request.RefreshTokenRequest;
import com.kkori.api.auth.dto.response.LogoutResponse;
import com.kkori.api.auth.dto.response.OAuthLoginResponse;
import com.kkori.api.auth.dto.response.RefreshTokenResponse;
import com.kkori.api.auth.entity.RevokedRefreshToken;
import com.kkori.api.auth.jwt.JwtClaims;
import com.kkori.api.auth.jwt.JwtTokenIssuer;
import com.kkori.api.auth.jwt.JwtTokenVerifier;
import com.kkori.api.auth.jwt.JwtTokenType;
import com.kkori.api.auth.oauth.OAuthUserInfo;
import com.kkori.api.auth.oauth.OAuthVerifier;
import com.kkori.api.auth.oauth.OAuthVerifierResolver;
import com.kkori.api.auth.repository.RevokedRefreshTokenRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.entity.Platform;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.entity.Species;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.user.entity.OAuthProvider;
import com.kkori.api.user.entity.User;
import com.kkori.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private OAuthVerifierResolver oauthVerifierResolver;

    @Mock
    private OAuthVerifier oauthVerifier;

    @Mock
    private JwtTokenIssuer jwtTokenIssuer;

    @Mock
    private JwtTokenVerifier jwtTokenVerifier;

    @Mock
    private RevokedRefreshTokenRepository revokedRefreshTokenRepository;

    @Mock
    private ProviderLogoutClient providerLogoutClient;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                deviceRepository,
                petRepository,
                oauthVerifierResolver,
                jwtTokenIssuer,
                jwtTokenVerifier,
                revokedRefreshTokenRepository,
                List.of(providerLogoutClient)
        );
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void oauthLoginCreatesUserConnectsDeviceAndMigratesDevicePets() {
        Device device = device(10L, "device-1");
        Pet pet = pet(20L, 10L, "pet-1");
        OAuthUserInfo userInfo = new OAuthUserInfo("google-1", "a@example.com", "kkori", null);

        when(oauthVerifierResolver.resolve(OAuthProvider.GOOGLE)).thenReturn(oauthVerifier);
        when(oauthVerifier.verify(any(OAuthLoginRequest.class))).thenReturn(userInfo);
        when(userRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-1"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        when(deviceRepository.findByExternalId("device-1")).thenReturn(Optional.of(device));
        when(petRepository.findByDeviceIdAndUserIdIsNull(10L)).thenReturn(List.of(pet));
        when(jwtTokenIssuer.issueAccessToken(any(User.class))).thenReturn("access");
        when(jwtTokenIssuer.issueRefreshToken(any(User.class))).thenReturn("refresh");

        OAuthLoginResponse response = authService.login(
                new OAuthLoginRequest(OAuthProvider.GOOGLE, "id-token", null, null, null, "device-1")
        );

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.user().provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(device.getUserId()).isEqualTo(1L);
        assertThat(pet.getUserId()).isEqualTo(1L);
    }

    @Test
    void oauthLoginReusesExistingUser() {
        User user = user(1L, OAuthProvider.KAKAO, "kakao-1");
        Device device = device(10L, "device-1");
        OAuthUserInfo userInfo = new OAuthUserInfo("kakao-1", null, "new-name", "https://example.com/p.png");

        when(oauthVerifierResolver.resolve(OAuthProvider.KAKAO)).thenReturn(oauthVerifier);
        when(oauthVerifier.verify(any(OAuthLoginRequest.class))).thenReturn(userInfo);
        when(userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "kakao-1"))
                .thenReturn(Optional.of(user));
        when(deviceRepository.findByExternalId("device-1")).thenReturn(Optional.of(device));
        when(petRepository.findByDeviceIdAndUserIdIsNull(10L)).thenReturn(List.of());
        when(jwtTokenIssuer.issueAccessToken(user)).thenReturn("access");
        when(jwtTokenIssuer.issueRefreshToken(user)).thenReturn("refresh");

        OAuthLoginResponse response = authService.login(
                new OAuthLoginRequest(OAuthProvider.KAKAO, null, "access-token", null, null, "device-1")
        );

        assertThat(response.user().externalId()).isEqualTo(user.getExternalId());
        assertThat(response.user().nickname()).isEqualTo("new-name");
        assertThat(device.getUserId()).isEqualTo(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refreshTokenIssuesNewAccessToken() {
        User user = user(1L, OAuthProvider.GOOGLE, "google-1");
        when(revokedRefreshTokenRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(jwtTokenVerifier.verifyRefreshToken("refresh-token"))
                .thenReturn(new JwtClaims(1L, "user-1", JwtTokenType.REFRESH));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenIssuer.issueAccessToken(user)).thenReturn("new-access");

        RefreshTokenResponse response = authService.refresh(new RefreshTokenRequest("refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-access");
    }

    @Test
    void refreshTokenRejectsRevokedToken() {
        when(revokedRefreshTokenRepository.existsByTokenHash(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("refresh-token")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_003));
    }

    @Test
    void logoutRevokesRefreshTokenKeepsDeviceUserMappingAndAttemptsKakaoLogout() {
        User user = user(1L, OAuthProvider.KAKAO, "kakao-1");
        Device device = device(10L, "device-1", 1L);
        AuthContext.set(new AuthenticatedUser(1L, "user-1"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenVerifier.verifyRefreshToken("refresh-token"))
                .thenReturn(new JwtClaims(1L, "user-1", JwtTokenType.REFRESH));
        when(revokedRefreshTokenRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(providerLogoutClient.supports(OAuthProvider.KAKAO)).thenReturn(true);
        when(providerLogoutClient.logout("kakao-access-token")).thenReturn(true);

        LogoutResponse response = authService.logout(new LogoutRequest(
                "device-1",
                "refresh-token",
                "kakao-access-token"
        ));

        assertThat(response.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(response.appSessionRevoked()).isTrue();
        assertThat(response.providerLogoutAttempted()).isTrue();
        assertThat(response.providerLogoutSuccess()).isTrue();
        assertThat(device.getUserId()).isEqualTo(1L);
        verify(revokedRefreshTokenRepository).save(any(RevokedRefreshToken.class));
    }

    @Test
    void googleLogoutSkipsProviderLogoutWhenProviderTokenIsNotStored() {
        User user = user(1L, OAuthProvider.GOOGLE, "google-1");
        AuthContext.set(new AuthenticatedUser(1L, "user-1"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        LogoutResponse response = authService.logout(new LogoutRequest(null, null, null));

        assertThat(response.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(response.appSessionRevoked()).isFalse();
        assertThat(response.providerLogoutAttempted()).isFalse();
        assertThat(response.providerLogoutSuccess()).isFalse();
        verifyNoInteractions(providerLogoutClient);
    }

    private User user(Long id, OAuthProvider provider, String providerUserId) {
        User user = User.builder()
                .externalId("user-1")
                .provider(provider)
                .providerUserId(providerUserId)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Device device(Long id, String externalId) {
        return device(id, externalId, null);
    }

    private Device device(Long id, String externalId, Long userId) {
        Device device = Device.builder()
                .externalId(externalId)
                .platform(Platform.IOS)
                .userId(userId)
                .build();
        ReflectionTestUtils.setField(device, "id", id);
        return device;
    }

    private Pet pet(Long id, Long deviceId, String externalId) {
        Pet pet = Pet.builder()
                .externalId(externalId)
                .deviceId(deviceId)
                .name("꼬리")
                .species(Species.DOG)
                .build();
        ReflectionTestUtils.setField(pet, "id", id);
        return pet;
    }
}
