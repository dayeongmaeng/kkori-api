package com.kkori.api.auth.entity;

import com.kkori.api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "revoked_refresh_token",
        indexes = {
                @Index(name = "idx_revoked_refresh_token_hash", columnList = "token_hash", unique = true)
        }
)
public class RevokedRefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    public RevokedRefreshToken(String tokenHash, Long userId) {
        this.tokenHash = tokenHash;
        this.userId = userId;
    }
}
