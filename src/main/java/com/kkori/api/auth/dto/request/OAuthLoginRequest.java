package com.kkori.api.auth.dto.request;

import com.kkori.api.user.entity.OAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthLoginRequest(
        @NotNull OAuthProvider provider,
        String idToken,
        String accessToken,
        String code,
        String redirectUri,
        @NotBlank String deviceExternalId,
        // Google OAuth token storage용 선택 필드.
        // 클라이언트가 전달하지 않아도 로그인은 정상 처리된다.
        String googleOAuthAccessToken,
        String googleRefreshToken
) {
    public String token() {
        if (idToken != null && !idToken.isBlank()) {
            return idToken;
        }
        return accessToken;
    }
}
