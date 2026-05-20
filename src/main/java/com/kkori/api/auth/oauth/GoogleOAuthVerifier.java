package com.kkori.api.auth.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.entity.OAuthProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@EnableConfigurationProperties(OAuthProperties.class)
public class GoogleOAuthVerifier implements OAuthVerifier {

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final RestClient restClient;
    private final OAuthProperties properties;

    public GoogleOAuthVerifier(RestClient.Builder restClientBuilder, OAuthProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        GoogleTokenInfoResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("oauth2.googleapis.com")
                            .path("/tokeninfo")
                            .queryParam("id_token", token)
                            .build())
                    .retrieve()
                    .body(GoogleTokenInfoResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        String clientId = resolveClientId();
        if (response == null || isBlank(response.sub()) || !clientId.equals(response.aud())) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        return new OAuthUserInfo(
                response.sub(),
                response.email(),
                response.name(),
                response.picture()
        );
    }

    private String resolveClientId() {
        if (properties.google() == null || isBlank(properties.google().clientId())) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        return properties.google().clientId();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GoogleTokenInfoResponse(
            String sub,
            String aud,
            String email,
            String name,
            String picture,
            @JsonProperty("email_verified") String emailVerified
    ) {
    }
}
