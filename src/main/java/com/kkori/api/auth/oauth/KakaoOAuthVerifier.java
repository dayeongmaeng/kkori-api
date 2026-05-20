package com.kkori.api.auth.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.entity.OAuthProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@EnableConfigurationProperties(OAuthProperties.class)
public class KakaoOAuthVerifier implements OAuthVerifier {

    private final RestClient restClient;
    private final OAuthProperties properties;

    public KakaoOAuthVerifier(RestClient.Builder restClientBuilder, OAuthProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo verify(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        validateConfigured();

        KakaoUserResponse response;
        try {
            response = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(KakaoUserResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        if (response == null || response.id() == null) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        KakaoAccount account = response.kakaoAccount();
        KakaoProfile profile = account == null ? null : account.profile();

        return new OAuthUserInfo(
                String.valueOf(response.id()),
                account == null ? null : account.email(),
                profile == null ? null : profile.nickname(),
                profile == null ? null : profile.profileImageUrl()
        );
    }

    private void validateConfigured() {
        if (properties.kakao() == null
                || isBlank(properties.kakao().restApiKey())
                || isBlank(properties.kakao().nativeAppKey())) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record KakaoUserResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoAccount(
            String email,
            KakaoProfile profile
    ) {
    }

    private record KakaoProfile(
            String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl
    ) {
    }
}
