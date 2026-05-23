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
import com.kkori.api.auth.entity.UserOAuthToken;
import com.kkori.api.auth.jwt.JwtClaims;
import com.kkori.api.auth.jwt.JwtTokenIssuer;
import com.kkori.api.auth.jwt.JwtTokenVerifier;
import com.kkori.api.auth.oauth.OAuthTokenEncryptor;
import com.kkori.api.auth.oauth.OAuthUserInfo;
import com.kkori.api.auth.oauth.OAuthVerifierResolver;
import com.kkori.api.auth.repository.RevokedRefreshTokenRepository;
import com.kkori.api.auth.repository.UserOAuthTokenRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.user.dto.response.UserResponse;
import com.kkori.api.user.entity.OAuthProvider;
import com.kkori.api.user.entity.User;
import com.kkori.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final PetRepository petRepository;
    private final OAuthVerifierResolver oauthVerifierResolver;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final JwtTokenVerifier jwtTokenVerifier;
    private final RevokedRefreshTokenRepository revokedRefreshTokenRepository;
    private final UserOAuthTokenRepository userOAuthTokenRepository;
    private final OAuthTokenEncryptor oAuthTokenEncryptor;
    private final List<ProviderLogoutClient> providerLogoutClients;

    @Transactional
    public OAuthLoginResponse login(OAuthLoginRequest request) {
        OAuthUserInfo userInfo = oauthVerifierResolver.resolve(request.provider()).verify(request);

        User user = userRepository.findByProviderAndProviderUserId(request.provider(), userInfo.providerUserId())
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createUser(request, userInfo));

        Device device = deviceRepository.findByExternalId(request.deviceExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
        device.connectUser(user.getId());

        petRepository.findByDeviceIdAndUserIdIsNull(device.getId())
                .forEach(pet -> pet.connectUser(user.getId()));

        storeGoogleOAuthTokenIfPresent(user, request);

        return new OAuthLoginResponse(
                jwtTokenIssuer.issueAccessToken(user),
                jwtTokenIssuer.issueRefreshToken(user),
                UserResponse.from(user)
        );
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        if (revokedRefreshTokenRepository.existsByTokenHash(hashToken(request.refreshToken()))) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }

        JwtClaims claims = jwtTokenVerifier.verifyRefreshToken(request.refreshToken());
        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        if (user.isDeleted() || user.isWithdrawn()) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }

        // TODO: Add refresh token rotation and server-side refresh token revocation.
        return new RefreshTokenResponse(jwtTokenIssuer.issueAccessToken(user));
    }

    @Transactional
    public LogoutResponse logout(LogoutRequest request) {
        LogoutRequest safeRequest = request == null ? new LogoutRequest(null, null, null) : request;
        AuthenticatedUser authenticatedUser = AuthContext.currentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_003));
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        boolean appSessionRevoked = revokeRefreshTokenIfPresent(safeRequest.refreshToken(), user.getId());

        ProviderLogoutResult providerLogoutResult = logoutProviderIfPossible(user, safeRequest);

        log.info(
                "Logout processed: provider={}, userId={}, deviceExternalId={}, appSessionRevoked={}, providerLogoutAttempted={}, providerLogoutSuccess={}",
                user.getProvider(),
                user.getId(),
                safeRequest.deviceExternalId(),
                appSessionRevoked,
                providerLogoutResult.attempted(),
                providerLogoutResult.success()
        );

        return new LogoutResponse(
                user.getProvider(),
                appSessionRevoked,
                providerLogoutResult.attempted(),
                providerLogoutResult.success()
        );
    }

    private void storeGoogleOAuthTokenIfPresent(User user, OAuthLoginRequest request) {
        if (request.provider() != OAuthProvider.GOOGLE) {
            return;
        }

        log.info("[OAuth][Google] storeGoogleOAuthTokenIfPresent called: userId={}, hasGoogleOAuthAccessToken={}, hasGoogleRefreshToken={}",
                user.getId(),
                !isBlank(request.googleOAuthAccessToken()),
                !isBlank(request.googleRefreshToken()));

        if (oAuthTokenEncryptor.isDisabled()) {
            log.warn("[OAuth][Google] encryptor disabled — skipping token storage: userId={}", user.getId());
            return;
        }

        boolean hasAccess = !isBlank(request.googleOAuthAccessToken());
        boolean hasRefresh = !isBlank(request.googleRefreshToken());
        if (!hasAccess && !hasRefresh) {
            log.info("[OAuth][Google] no token provided — skipping token storage: userId={}", user.getId());
            return;
        }

        try {
            String encryptedAccess = hasAccess ? oAuthTokenEncryptor.encrypt(request.googleOAuthAccessToken()) : null;
            String encryptedRefresh = hasRefresh ? oAuthTokenEncryptor.encrypt(request.googleRefreshToken()) : null;

            userOAuthTokenRepository.findByUserIdAndProvider(user.getId(), OAuthProvider.GOOGLE)
                    .ifPresentOrElse(
                            existing -> existing.update(encryptedAccess, encryptedRefresh, null, null),
                            () -> userOAuthTokenRepository.save(UserOAuthToken.builder()
                                    .userId(user.getId())
                                    .provider(OAuthProvider.GOOGLE)
                                    .encryptedAccessToken(encryptedAccess)
                                    .encryptedRefreshToken(encryptedRefresh)
                                    .build())
                    );

            log.info("[OAuth][Google] token stored: userId={}", user.getId());
        } catch (Exception e) {
            log.error("[OAuth][Google] token storage failed, login continues: userId={}", user.getId(), e);
        }
    }

    private User updateExistingUser(User user, OAuthUserInfo userInfo) {
        user.updateProfile(userInfo.email(), userInfo.nickname(), userInfo.profileImageUrl());
        return user;
    }

    private User createUser(OAuthLoginRequest request, OAuthUserInfo userInfo) {
        User user = User.builder()
                .externalId(UUID.randomUUID().toString())
                .provider(request.provider())
                .providerUserId(userInfo.providerUserId())
                .email(userInfo.email())
                .nickname(userInfo.nickname())
                .profileImageUrl(userInfo.profileImageUrl())
                .build();
        return userRepository.save(user);
    }

    private boolean revokeRefreshTokenIfPresent(String refreshToken, Long userId) {
        if (isBlank(refreshToken)) {
            return false;
        }

        JwtClaims claims;
        try {
            claims = jwtTokenVerifier.verifyRefreshToken(refreshToken);
        } catch (BusinessException e) {
            return false;
        }

        if (!userId.equals(claims.userId())) {
            return false;
        }

        String tokenHash = hashToken(refreshToken);
        if (!revokedRefreshTokenRepository.existsByTokenHash(tokenHash)) {
            revokedRefreshTokenRepository.save(RevokedRefreshToken.builder()
                    .tokenHash(tokenHash)
                    .userId(userId)
                    .build());
        }
        return true;
    }

    private ProviderLogoutResult logoutProviderIfPossible(User user, LogoutRequest request) {
        if (user.getProvider() == null) {
            return new ProviderLogoutResult(false, false);
        }
        String providerAccessToken = switch (user.getProvider()) {
            case KAKAO -> request.kakaoAccessToken();
            case GOOGLE, APPLE -> null;
        };

        if (isBlank(providerAccessToken)) {
            return new ProviderLogoutResult(false, false);
        }

        return providerLogoutClients.stream()
                .filter(client -> client.supports(user.getProvider()))
                .findFirst()
                .map(client -> new ProviderLogoutResult(true, client.logout(providerAccessToken)))
                .orElseGet(() -> new ProviderLogoutResult(false, false));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ProviderLogoutResult(
            boolean attempted,
            boolean success
    ) {
    }
}
