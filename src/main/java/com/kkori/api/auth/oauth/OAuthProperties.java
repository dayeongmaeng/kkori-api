package com.kkori.api.auth.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.oauth")
public record OAuthProperties(
        Google google,
        Kakao kakao,
        Token token
) {
    public record Google(
            String clientId
    ) {
    }

    public record Kakao(
            String restApiKey,
            String nativeAppKey,
            String adminKey
    ) {
    }

    public record Token(
            String encryptionKey
    ) {
    }
}
