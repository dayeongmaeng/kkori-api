package com.kkori.api.auth.oauth.disconnect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkori.api.auth.oauth.OAuthProperties;
import com.kkori.api.user.entity.OAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@EnableConfigurationProperties(OAuthProperties.class)
public class KakaoOAuthDisconnectService implements OAuthDisconnectService {

    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";
    // Kakao error code -101: 존재하지 않는 사용자 (이미 연결 해제되었거나 미가입)
    private static final int KAKAO_CODE_INVALID_USER = -101;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final OAuthProperties properties;

    public KakaoOAuthDisconnectService(RestClient.Builder restClientBuilder, OAuthProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public boolean supports(OAuthProvider provider) {
        return provider == OAuthProvider.KAKAO;
    }

    @Override
    public boolean disconnect(Long userId, String providerUserId) {
        String adminKey = resolveAdminKey();
        if (adminKey == null) {
            log.warn("[OAuth][Kakao] unlink skipped: KAKAO_ADMIN_KEY not configured, userId={}", userId);
            return false;
        }

        log.info("[OAuth][Kakao] unlink start: userId={}, providerUserId={}", userId, providerUserId);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", providerUserId);

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(UNLINK_URL)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(String.class);

            log.info("[OAuth][Kakao] unlink success: userId={}, providerUserId={}, responseStatus={}",
                    userId, providerUserId, response.getStatusCode().value());
            return true;

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            int kakaoCode = parseKakaoCode(body);

            // 이미 연결 해제되었거나 존재하지 않는 사용자 → idempotent 처리
            if (kakaoCode == KAKAO_CODE_INVALID_USER) {
                log.info("[OAuth][Kakao] unlink skipped (already unlinked or user not found): userId={}, providerUserId={}, status={}, kakaoCode={}",
                        userId, providerUserId, status, kakaoCode);
                return true;
            }

            log.warn("[OAuth][Kakao] unlink failed: userId={}, providerUserId={}, status={}, message={}",
                    userId, providerUserId, status, body);
            return false;

        } catch (RestClientException e) {
            log.error("[OAuth][Kakao] unlink failed: userId={}, providerUserId={}, exception={}",
                    userId, providerUserId, e.getMessage());
            return false;
        }
    }

    private String resolveAdminKey() {
        if (properties.kakao() == null) {
            return null;
        }
        String key = properties.kakao().adminKey();
        return (key == null || key.isBlank()) ? null : key;
    }

    private int parseKakaoCode(String body) {
        if (body == null || body.isBlank()) {
            return 0;
        }
        try {
            JsonNode json = OBJECT_MAPPER.readTree(body);
            return json.path("code").asInt(0);
        } catch (Exception e) {
            return 0;
        }
    }
}
