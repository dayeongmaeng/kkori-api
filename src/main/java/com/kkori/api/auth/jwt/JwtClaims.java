package com.kkori.api.auth.jwt;

public record JwtClaims(
        Long userId,
        String userExternalId,
        JwtTokenType tokenType
) {
}
