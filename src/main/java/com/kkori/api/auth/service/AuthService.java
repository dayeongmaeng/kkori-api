package com.kkori.api.auth.service;

import com.kkori.api.auth.dto.request.OAuthLoginRequest;
import com.kkori.api.auth.dto.request.RefreshTokenRequest;
import com.kkori.api.auth.dto.response.OAuthLoginResponse;
import com.kkori.api.auth.dto.response.RefreshTokenResponse;
import com.kkori.api.auth.jwt.JwtClaims;
import com.kkori.api.auth.jwt.JwtTokenIssuer;
import com.kkori.api.auth.jwt.JwtTokenVerifier;
import com.kkori.api.auth.oauth.OAuthUserInfo;
import com.kkori.api.auth.oauth.OAuthVerifierResolver;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.user.dto.response.UserResponse;
import com.kkori.api.user.entity.User;
import com.kkori.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

    @Transactional
    public OAuthLoginResponse login(OAuthLoginRequest request) {
        OAuthUserInfo userInfo = oauthVerifierResolver.resolve(request.provider()).verify(request.token());

        User user = userRepository.findByProviderAndProviderUserId(request.provider(), userInfo.providerUserId())
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createUser(request, userInfo));

        Device device = deviceRepository.findByExternalId(request.deviceExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
        device.connectUser(user.getId());

        petRepository.findByDeviceIdAndUserIdIsNull(device.getId())
                .forEach(pet -> pet.connectUser(user.getId()));

        return new OAuthLoginResponse(
                jwtTokenIssuer.issueAccessToken(user),
                jwtTokenIssuer.issueRefreshToken(user),
                UserResponse.from(user)
        );
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        JwtClaims claims = jwtTokenVerifier.verifyRefreshToken(request.refreshToken());
        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        // TODO: Add refresh token rotation and server-side refresh token revocation.
        return new RefreshTokenResponse(jwtTokenIssuer.issueAccessToken(user));
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
}
