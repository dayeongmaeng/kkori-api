package com.kkori.api.auth.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.entity.OAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@EnableConfigurationProperties(OAuthProperties.class)
public class GoogleOAuthVerifier implements OAuthVerifier {

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
            log.warn("OAuth verification failed: provider=GOOGLE reason=token_blank");
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
        } catch (RestClientResponseException e) {
            log.warn("OAuth verification failed: provider=GOOGLE reason=tokeninfo_http_error status={}",
                    e.getStatusCode().value());
            throw new BusinessException(ErrorCode.AUTH_002);
        } catch (RestClientException e) {
            log.warn("OAuth verification failed: provider=GOOGLE reason=tokeninfo_request_error");
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        if (response == null || isBlank(response.sub())) {
            log.warn("OAuth verification failed: provider=GOOGLE reason=tokeninfo_response_invalid");
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        List<ClientIdEntry> allowedIds = resolveAllowedClientIds();
        ClientIdEntry matched = allowedIds.stream()
                .filter(entry -> entry.clientId().equals(response.aud()))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            log.warn("OAuth verification failed: provider=GOOGLE reason=audience_mismatch audience={} allowedTypes={}",
                    response.aud(), allowedIds.stream().map(e -> e.type().name()).toList());
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        log.info("OAuth verification succeeded: provider=GOOGLE clientType={}", matched.type());

        return new OAuthUserInfo(
                response.sub(),
                response.email(),
                response.name(),
                response.picture()
        );
    }

    private List<ClientIdEntry> resolveAllowedClientIds() {
        List<ClientIdEntry> allowed = new ArrayList<>();
        OAuthProperties.Google google = properties.google();
        if (google != null) {
            if (!isBlank(google.clientId())) {
                allowed.add(new ClientIdEntry(google.clientId(), ClientType.WEB));
            }
            if (!isBlank(google.iosClientId())) {
                allowed.add(new ClientIdEntry(google.iosClientId(), ClientType.IOS));
            }
        }
        if (allowed.isEmpty()) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        return allowed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private enum ClientType {
        WEB, IOS
    }

    private record ClientIdEntry(String clientId, ClientType type) {}

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
