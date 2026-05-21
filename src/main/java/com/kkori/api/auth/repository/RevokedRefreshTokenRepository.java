package com.kkori.api.auth.repository;

import com.kkori.api.auth.entity.RevokedRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedRefreshTokenRepository extends JpaRepository<RevokedRefreshToken, Long> {

    boolean existsByTokenHash(String tokenHash);
}
