package com.kkori.api.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kkori.api.auth.dto.request.OAuthLoginRequest;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.entity.OAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@EnableConfigurationProperties(OAuthProperties.class)
@Slf4j
public class KakaoOAuthVerifier implements OAuthVerifier {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_ME_URL = "https://kapi.kakao.com/v2/user/me";
    private static final MediaType FORM_URLENCODED_UTF8 =
            MediaType.parseMediaType("application/x-www-form-urlencoded;charset=UTF-8");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final OAuthProperties properties;

    public KakaoOAuthVerifier(RestClient.Builder restClientBuilder, OAuthProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo verify(OAuthLoginRequest request) {
        if (!isBlank(request.code())) {
            validateConfigured();
            if (isBlank(request.redirectUri())) {
                log.warn("Kakao OAuth failed: redirectUri is blank");
                throw new BusinessException(ErrorCode.AUTH_002);
            }

            String accessToken = exchangeCodeForAccessToken(request.code(), request.redirectUri());
            return fetchUserInfo(accessToken);
        }

        return verify(request.token());
    }

    @Override
    public OAuthUserInfo verify(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Kakao OAuth failed: code/accessToken is blank");
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        validateConfigured();
        return fetchUserInfo(token);
    }

    private String exchangeCodeForAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.kakao().restApiKey());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        log.info("Kakao OAuth token exchange request: redirectUri={}, restApiKey={}",
                redirectUri, mask(properties.kakao().restApiKey()));

        KakaoTokenResponse response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(FORM_URLENCODED_UTF8)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
        } catch (RestClientResponseException e) {
            logKakaoError("token endpoint", e);
            throw new BusinessException(ErrorCode.AUTH_002);
        } catch (RestClientException e) {
            log.warn("Kakao OAuth failed: token endpoint request error");
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        if (response == null || isBlank(response.accessToken())) {
            log.warn("Kakao OAuth failed: token response has no access_token");
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        log.info("Kakao OAuth token exchange succeeded: accessTokenPresent=true");
        return response.accessToken();
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        KakaoUserResponse response;
        try {
            response = restClient.get()
                    .uri(USER_ME_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
        } catch (RestClientResponseException e) {
            logKakaoError("user/me", e);
            throw new BusinessException(ErrorCode.AUTH_002);
        } catch (RestClientException e) {
            log.warn("Kakao OAuth failed: user/me request error");
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        if (response == null || response.id() == null) {
            log.warn("Kakao OAuth failed: user response has no id");
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        KakaoAccount account = response.kakaoAccount();
        KakaoProfile profile = account == null ? null : account.profile();

        return new OAuthUserInfo(
                String.valueOf(response.id()),
                account == null ? null : account.email(),
                profile == null ? null : profile.nickname(),
                profile == null ? null : profile.profileImageUrl()
        );
    }

    private void validateConfigured() {
        if (properties.kakao() == null
                || isBlank(properties.kakao().restApiKey())) {
            log.warn("Kakao OAuth failed: KAKAO_REST_API_KEY is missing or blank");
            throw new BusinessException(ErrorCode.AUTH_002);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String mask(String value) {
        if (isBlank(value)) {
            return "<blank>";
        }
        int visibleLength = Math.min(6, value.length());
        return value.substring(0, visibleLength) + "***";
    }

    private void logKakaoError(String step, RestClientResponseException e) {
        KakaoError error = parseKakaoError(e.getResponseBodyAsString());
        log.warn("Kakao OAuth failed: step={}, status={}, error={}, errorDescription={}",
                step,
                e.getStatusCode().value(),
                error.error(),
                error.errorDescription());
    }

    private KakaoError parseKakaoError(String body) {
        if (isBlank(body)) {
            return new KakaoError(null, null);
        }

        try {
            JsonNode json = OBJECT_MAPPER.readTree(body);
            return new KakaoError(
                    json.path("error").isMissingNode() ? null : json.path("error").asText(null),
                    json.path("error_description").isMissingNode()
                            ? null
                            : json.path("error_description").asText(null)
            );
        } catch (Exception e) {
            return new KakaoError("<unparseable>", null);
        }
    }

    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    private record KakaoError(
            String error,
            String errorDescription
    ) {
    }

    private record KakaoUserResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoAccount(
            String email,
            KakaoProfile profile
    ) {
    }

    private record KakaoProfile(
            String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl
    ) {
    }
}
