package com.kkori.api.auth.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtClaims verifyAccessToken(String token) {
        return verify(token, JwtTokenType.ACCESS);
    }

    public JwtClaims verifyRefreshToken(String token) {
        return verify(token, JwtTokenType.REFRESH);
    }

    private JwtClaims verify(String token, JwtTokenType expectedType) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }

        String unsigned = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsigned), parts[2])) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }

        Map<String, Object> payload = readPayload(parts[1]);
        String type = stringClaim(payload, "type");
        if (!expectedType.value().equals(type)) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }

        long exp = longClaim(payload, "exp");
        if (Instant.now().getEpochSecond() >= exp) {
            throw new BusinessException(ErrorCode.AUTH_004);
        }

        return new JwtClaims(
                longClaim(payload, "uid"),
                stringClaim(payload, "sub"),
                expectedType
        );
    }

    private Map<String, Object> readPayload(String encodedPayload) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(decoded, MAP_TYPE);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }
    }

    private String stringClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }
        return stringValue;
    }

    private long longClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw new BusinessException(ErrorCode.AUTH_003);
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigestHolder.equals(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MessageDigestHolder {
        private static boolean equals(byte[] expected, byte[] actual) {
            return java.security.MessageDigest.isEqual(expected, actual);
        }
    }
}
