package com.kkori.api.auth.dto.request;

import com.kkori.api.user.entity.OAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthLoginRequest(
        @NotNull OAuthProvider provider,
        String idToken,
        String accessToken,
        @NotBlank String deviceExternalId
) {
    public String token() {
        if (idToken != null && !idToken.isBlank()) {
            return idToken;
        }
        return accessToken;
    }
}
