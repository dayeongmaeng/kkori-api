package com.kkori.api.auth.repository;

import com.kkori.api.auth.entity.UserOAuthToken;
import com.kkori.api.user.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOAuthTokenRepository extends JpaRepository<UserOAuthToken, Long> {

    Optional<UserOAuthToken> findByUserIdAndProvider(Long userId, OAuthProvider provider);
}
