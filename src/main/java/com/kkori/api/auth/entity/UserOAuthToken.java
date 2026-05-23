package com.kkori.api.auth.entity;

import com.kkori.api.common.entity.BaseEntity;
import com.kkori.api.user.entity.OAuthProvider;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "user_oauth_token",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_oauth_token_user_provider",
                        columnNames = {"user_id", "provider"}
                )
        }
)
public class UserOAuthToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    // AES-256-GCM 암호화 저장. null이면 해당 토큰 미보유.
    @Column(name = "encrypted_access_token", columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "access_token_expires_at")
    private LocalDateTime accessTokenExpiresAt;

    @Column(name = "scope")
    private String scope;

    // revoke 완료 시각. null이면 아직 활성 상태.
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Builder
    public UserOAuthToken(Long userId, OAuthProvider provider,
                          String encryptedAccessToken, String encryptedRefreshToken,
                          LocalDateTime accessTokenExpiresAt, String scope) {
        this.userId = userId;
        this.provider = provider;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.scope = scope;
    }

    public void update(String encryptedAccessToken, String encryptedRefreshToken,
                       LocalDateTime accessTokenExpiresAt, String scope) {
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.scope = scope;
        this.revokedAt = null;
    }

    public void markRevoked() {
        this.revokedAt = LocalDateTime.now();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
