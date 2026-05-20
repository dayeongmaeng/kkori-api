package com.kkori.api.auth.dto.response;

import com.kkori.api.user.dto.response.UserResponse;

public record OAuthLoginResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
