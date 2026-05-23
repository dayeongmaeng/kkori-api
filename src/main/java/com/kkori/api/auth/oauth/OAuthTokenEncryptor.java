package com.kkori.api.auth.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 기반 OAuth token 암복호화.
 * OAUTH_TOKEN_ENCRYPTION_KEY가 없거나 32바이트 미만이면 비활성화(disabled=true).
 * 비활성화 시 encrypt/decrypt 호출은 null 반환 → 토큰 저장 기능이 조용히 스킵된다.
 */
@Slf4j
@Component
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthTokenEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_BYTES = 32;

    private final SecretKey secretKey;
    private final boolean disabled;

    public OAuthTokenEncryptor(OAuthProperties properties) {
        SecretKey resolved = null;
        boolean skip = true;
        try {
            String raw = (properties.token() != null) ? properties.token().encryptionKey() : null;
            if (raw != null && !raw.isBlank()) {
                byte[] keyBytes = raw.getBytes(StandardCharsets.UTF_8);
                if (keyBytes.length >= KEY_BYTES) {
                    resolved = new SecretKeySpec(Arrays.copyOf(keyBytes, KEY_BYTES), "AES");
                    skip = false;
                    log.info("[OAuthTokenEncryptor] initialized: enabled=true, keyLength={}", keyBytes.length);
                } else {
                    log.warn("[OAuthTokenEncryptor] OAUTH_TOKEN_ENCRYPTION_KEY is shorter than 32 bytes (keyLength={}) — OAuth token storage disabled", keyBytes.length);
                }
            } else {
                log.warn("[OAuthTokenEncryptor] OAUTH_TOKEN_ENCRYPTION_KEY not configured — OAuth token storage disabled");
            }
        } catch (Exception e) {
            log.error("[OAuthTokenEncryptor] failed to initialize encryption key — OAuth token storage disabled", e);
        }
        this.secretKey = resolved;
        this.disabled = skip;
    }

    public boolean isDisabled() {
        return disabled;
    }

    /**
     * 평문 token을 암호화한다. 비활성화 상태이면 null 반환.
     * 반환 형식: Base64(IV || ciphertext+tag)
     */
    public String encrypt(String plaintext) {
        if (disabled || plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("[OAuthTokenEncryptor] encrypt failed");
            return null;
        }
    }

    /**
     * 암호화된 token을 복호화한다. 비활성화 상태이거나 복호화 실패 시 null 반환.
     */
    public String decrypt(String encrypted) {
        if (disabled || encrypted == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = Arrays.copyOf(combined, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[OAuthTokenEncryptor] decrypt failed");
            return null;
        }
    }
}
