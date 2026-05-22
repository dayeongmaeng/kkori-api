package com.kkori.api.user.repository;

import com.kkori.api.user.entity.OAuthProvider;
import com.kkori.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    Optional<User> findByProviderAndProviderUserIdAndDeletedAtIsNull(OAuthProvider provider, String providerUserId);
}
