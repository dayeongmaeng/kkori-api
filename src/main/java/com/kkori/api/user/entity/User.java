package com.kkori.api.user.entity;

import com.kkori.api.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
