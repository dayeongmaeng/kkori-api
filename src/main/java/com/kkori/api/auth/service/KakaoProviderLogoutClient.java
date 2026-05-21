package com.kkori.api.auth.service;

import com.kkori.api.user.entity.OAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoProviderLogoutClient implements ProviderLogoutClient {

    private static final String LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";

    private final RestClient restClient;

    public KakaoProviderLogoutClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean supports(OAuthProvider provider) {
        return provider == OAuthProvider.KAKAO;
    }

    @Override
    public boolean logout(String providerAccessToken) {
        if (providerAccessToken == null || providerAccessToken.isBlank()) {
            return false;
        }

        try {
            restClient.post()
                    .uri(LOGOUT_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerAccessToken)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("Kakao provider logout failed");
            return false;
        }
    }
}
