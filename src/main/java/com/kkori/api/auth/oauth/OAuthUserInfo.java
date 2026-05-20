package com.kkori.api.auth.oauth;

public record OAuthUserInfo(
        String providerUserId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
