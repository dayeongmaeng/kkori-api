package com.kkori.api.auth.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenIssuer {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public String issueAccessToken(User user) {
        return issue(user, JwtTokenType.ACCESS, properties.accessTokenTtlSeconds());
    }

    public String issueRefreshToken(User user) {
        return issue(user, JwtTokenType.REFRESH, properties.refreshTokenTtlSeconds());
    }

    private String issue(User user, JwtTokenType tokenType, long ttlSeconds) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getExternalId());
        payload.put("uid", user.getId());
        payload.put("provider", user.getProvider().name());
        payload.put("type", tokenType.value());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(ttlSeconds).getEpochSecond());

        String unsigned = base64UrlJson(header) + "." + base64UrlJson(payload);
        return unsigned + "." + sign(unsigned);
    }

    private String base64UrlJson(Object value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
    }
}
