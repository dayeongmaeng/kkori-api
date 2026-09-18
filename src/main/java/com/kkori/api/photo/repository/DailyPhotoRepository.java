package com.kkori.api.photo.repository;

import com.kkori.api.photo.entity.DailyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DailyPhotoRepository extends JpaRepository<DailyPhoto, Long> {
    Optional<DailyPhoto> findByExternalId(String externalId);
    boolean existsByExternalId(String externalId);
    List<DailyPhoto> findByPetId(Long petId);
    boolean existsByPetIdAndDate(Long petId, LocalDate date);

    Optional<DailyPhoto> findByExternalIdAndDeletedAtIsNull(String externalId);
    List<DailyPhoto> findByPetIdAndDeletedAtIsNull(Long petId);
    boolean existsByPetIdAndDateAndDeletedAtIsNull(Long petId, LocalDate date);

    // 대시보드 집계용. created_at 기준(활성 판정)과 date 기준(누적/streak)을 각각 지원한다.
    List<DailyPhoto> findByCreatedAtBetween(LocalDateTime start, LocalDateTime endExclusive);
    List<DailyPhoto> findByDateBetween(LocalDate start, LocalDate endInclusive);
    List<DailyPhoto> findByPetIdInAndDateBetween(Collection<Long> petIds, LocalDate start, LocalDate endInclusive);
    List<DailyPhoto> findByPetIdIn(Collection<Long> petIds);
}
