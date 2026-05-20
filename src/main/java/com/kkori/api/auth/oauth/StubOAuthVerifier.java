package com.kkori.api.auth.oauth;

import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.entity.OAuthProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

abstract class StubOAuthVerifier implements OAuthVerifier {

    @Override
    public OAuthUserInfo verify(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        // TODO: Replace with provider-specific token verification before production auth rollout.
        String providerUserId = token.startsWith("stub:")
                ? token.substring("stub:".length())
                : sha256(token);
        return new OAuthUserInfo(providerUserId, null, null, null);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
    }
}
