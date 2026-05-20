package com.kkori.api.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        String secret,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds
) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 환경변수가 설정되지 않았습니다.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET은 최소 32자 이상이어야 합니다.");
        }
        if (accessTokenTtlSeconds <= 0) {
            accessTokenTtlSeconds = 60 * 60;
        }
        if (refreshTokenTtlSeconds <= 0) {
            refreshTokenTtlSeconds = 60L * 60 * 24 * 30;
        }
    }
}
