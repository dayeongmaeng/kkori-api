package com.kkori.api.auth.oauth.disconnect;

import com.kkori.api.auth.entity.UserOAuthToken;
import com.kkori.api.auth.oauth.OAuthTokenEncryptor;
import com.kkori.api.auth.repository.UserOAuthTokenRepository;
import com.kkori.api.user.entity.OAuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthDisconnectService implements OAuthDisconnectService {

    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";

    private final UserOAuthTokenRepository userOAuthTokenRepository;
    private final OAuthTokenEncryptor oAuthTokenEncryptor;
    private final RestClient.Builder restClientBuilder;

    @Override
    public boolean supports(OAuthProvider provider) {
        return provider == OAuthProvider.GOOGLE;
    }

    @Override
    public boolean disconnect(Long userId, String providerUserId) {
        Optional<UserOAuthToken> tokenOpt = userOAuthTokenRepository.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE);

        if (tokenOpt.isEmpty()) {
            log.info("[OAuth][Google] revoke skipped: no stored token, userId={}", userId);
            return false;
        }

        UserOAuthToken token = tokenOpt.get();

        if (token.isRevoked()) {
            log.info("[OAuth][Google] revoke skipped: already revoked, userId={}", userId);
            return true;
        }

        String plainToken = resolveRevokeToken(token);
        if (plainToken == null) {
            log.warn("[OAuth][Google] revoke skipped: no decryptable token available, userId={}", userId);
            return false;
        }

        return doRevoke(userId, token, plainToken);
    }

    private String resolveRevokeToken(UserOAuthToken token) {
        // refreshToken 우선 (만료 없음)
        if (token.getEncryptedRefreshToken() != null) {
            String decrypted = oAuthTokenEncryptor.decrypt(token.getEncryptedRefreshToken());
            if (decrypted != null) {
                return decrypted;
            }
        }
        if (token.getEncryptedAccessToken() != null) {
            String decrypted = oAuthTokenEncryptor.decrypt(token.getEncryptedAccessToken());
            if (decrypted != null) {
                return decrypted;
            }
        }
        return null;
    }

    private boolean doRevoke(Long userId, UserOAuthToken token, String plainToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", plainToken);

        try {
            restClientBuilder.build()
                    .post()
                    .uri(REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            token.markRevoked();
            userOAuthTokenRepository.save(token);
            log.info("[OAuth][Google] revoke success: userId={}", userId);
            return true;

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();

            // invalid_token: 이미 만료/취소된 토큰 → idempotent 처리
            if (status == HttpStatus.BAD_REQUEST.value() && body != null && body.contains("invalid_token")) {
                token.markRevoked();
                userOAuthTokenRepository.save(token);
                log.info("[OAuth][Google] revoke skipped (token already invalid): userId={}", userId);
                return true;
            }

            log.warn("[OAuth][Google] revoke failed: userId={}, status={}", userId, status);
            return false;

        } catch (Exception e) {
            log.error("[OAuth][Google] revoke failed: userId={}", userId, e);
            return false;
        }
    }
}
