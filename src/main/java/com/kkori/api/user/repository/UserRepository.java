package com.kkori.api.user.repository;

import com.kkori.api.user.entity.OAuthProvider;
import com.kkori.api.user.entity.User;
import com.kkori.api.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    Optional<User> findByProviderAndProviderUserIdAndDeletedAtIsNull(OAuthProvider provider, String providerUserId);

    Optional<User> findByExternalIdAndDeletedAtIsNull(String externalId);

    // 대시보드 집계용. status는 탈퇴(withdraw())로만 deletedAt이 세팅되므로
    // ACTIVE/SUSPENDED 카운트에는 별도 deletedAt 조건이 필요 없다.
    List<User> findByStatus(UserStatus status);
    long countByStatus(UserStatus status);
    List<User> findByDeletedAtIsNull();
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime endExclusive);
    List<User> findByDeletedAtBetween(LocalDateTime start, LocalDateTime endExclusive);

    // keyword는 항상 빈 문자열 이상으로 정규화해서 넘긴다 ("" LIKE '%%'는 전체 매칭).
    // :keyword에 null이 들어오면 LOWER()/CONCAT() 안에서만 쓰이는 파라미터 타입을 Hibernate가
    // 추론하지 못해 Postgres에 bytea로 바인딩되어 "function lower(bytea) does not exist" 에러가 난다.
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL "
            + "AND (LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchActive(@Param("keyword") String keyword, Pageable pageable);
}
