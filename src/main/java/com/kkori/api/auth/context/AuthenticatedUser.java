package com.kkori.api.auth.context;

public record AuthenticatedUser(
        Long userId,
        String userExternalId
) {
}
