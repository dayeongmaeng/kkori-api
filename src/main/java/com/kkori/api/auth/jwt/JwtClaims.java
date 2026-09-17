package com.kkori.api.auth.jwt;

import java.time.Instant;

public record JwtClaims(
        Long userId,
        String userExternalId,
        JwtTokenType tokenType,
        Instant issuedAt
) {
}
