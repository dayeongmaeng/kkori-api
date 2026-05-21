package com.kkori.api.auth.dto.request;

public record LogoutRequest(
        String deviceExternalId,
        String refreshToken,
        String kakaoAccessToken
) {
}
