package com.kkori.api.user.entity;

import com.kkori.api.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
public class User extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    // nullable: provider/providerUserId are nulled out on withdrawal for re-registration support.
    // PostgreSQL treats (NULL, NULL) as distinct in unique constraints, so re-registration is safe.
    // DB migration required: ALTER TABLE users ALTER COLUMN provider DROP NOT NULL;
    //                        ALTER TABLE users ALTER COLUMN provider_user_id DROP NOT NULL;
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    @Column(name = "provider_user_id")
    private String providerUserId;

    private String email;

    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "suspension_reason")
    private String suspensionReason;

    // refresh token의 "iat" 클레임(epoch seconds)과 직접 비교하기 위해 Instant로 저장한다.
    // 이 값 이전에 발급된 refresh token은 모두 무효로 취급한다 (관리자 강제 로그아웃).
    @Column(name = "session_invalidated_at")
    private Instant sessionInvalidatedAt;

    @Builder
    public User(String externalId, OAuthProvider provider, String providerUserId,
                String email, String nickname, String profileImageUrl) {
        this.externalId = externalId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.status = UserStatus.ACTIVE;
    }

    public void updateProfile(String email, String nickname, String profileImageUrl) {
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.email = null;
        this.nickname = "탈퇴한 사용자";
        this.profileImageUrl = null;
        this.provider = null;
        this.providerUserId = null;
        softDelete();
    }

    public boolean isWithdrawn() {
        return UserStatus.WITHDRAWN.equals(this.status);
    }

    public boolean isSuspended() {
        return UserStatus.SUSPENDED.equals(this.status);
    }

    /** 정지와 함께 기존 세션도 끊는다 (모든 refresh token을 이 시점 이후 발급분만 유효하게 만든다). */
    public void suspend(String reason) {
        this.status = UserStatus.SUSPENDED;
        this.suspendedAt = LocalDateTime.now();
        this.suspensionReason = reason;
        this.sessionInvalidatedAt = Instant.now();
    }

    public void forceLogout() {
        this.sessionInvalidatedAt = Instant.now();
    }

    public boolean isSessionInvalidatedAfter(Instant tokenIssuedAt) {
        return sessionInvalidatedAt != null && tokenIssuedAt.isBefore(sessionInvalidatedAt);
    }
}
