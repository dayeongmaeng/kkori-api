package com.kkori.api.pet.repository;

import com.kkori.api.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    Optional<Pet> findByExternalId(String externalId);
    boolean existsByExternalId(String externalId);
    List<Pet> findByDeviceId(Long deviceId);
    List<Pet> findByUserId(Long userId);
    List<Pet> findByDeviceIdAndUserIdIsNull(Long deviceId);
    Optional<Pet> findByExternalIdAndDeviceId(String externalId, Long deviceId);
    Optional<Pet> findByExternalIdAndDeviceIdAndUserIdIsNull(String externalId, Long deviceId);
    Optional<Pet> findByExternalIdAndUserId(String externalId, Long userId);

    Optional<Pet> findByExternalIdAndDeletedAtIsNull(String externalId);
    List<Pet> findByDeviceIdAndDeletedAtIsNull(Long deviceId);
    List<Pet> findByUserIdAndDeletedAtIsNull(Long userId);
    List<Pet> findByDeviceIdAndUserIdIsNullAndDeletedAtIsNull(Long deviceId);
    Optional<Pet> findByExternalIdAndDeviceIdAndDeletedAtIsNull(String externalId, Long deviceId);
    Optional<Pet> findByExternalIdAndDeviceIdAndUserIdIsNullAndDeletedAtIsNull(String externalId, Long deviceId);
    Optional<Pet> findByExternalIdAndUserIdAndDeletedAtIsNull(String externalId, Long userId);

    long countByUserIdAndDeletedAtIsNull(Long userId);
    long countByDeviceIdAndDeletedAtIsNull(Long deviceId);

    // 대시보드 집계용. bound = 조회 기간 종료일(to) 다음날 KST 자정의 UTC 경계값 -> "to 시점에 이미 생성된 펫".
    List<Pet> findByCreatedAtLessThan(LocalDateTime bound);
    List<Pet> findByCreatedAtBetween(LocalDateTime start, LocalDateTime endExclusive);
    List<Pet> findByUserIdIn(Collection<Long> userIds);
}
