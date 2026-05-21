package com.kkori.api.auth.dto.response;

import com.kkori.api.user.entity.OAuthProvider;

public record LogoutResponse(
        OAuthProvider provider,
        boolean appSessionRevoked,
        boolean providerLogoutAttempted,
        boolean providerLogoutSuccess
) {
}
