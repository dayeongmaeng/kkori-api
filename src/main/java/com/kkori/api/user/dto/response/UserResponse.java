package com.kkori.api.user.dto.response;

import com.kkori.api.user.entity.OAuthProvider;
import com.kkori.api.user.entity.User;

public record UserResponse(
        String externalId,
        OAuthProvider provider,
        String email,
        String nickname,
        String profileImageUrl
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getExternalId(),
                user.getProvider(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
