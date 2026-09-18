package com.kkori.api.log.repository;

import com.kkori.api.log.entity.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {
    Optional<DailyLog> findByExternalId(String externalId);
    boolean existsByExternalId(String externalId);
    List<DailyLog> findByPetId(Long petId);
    boolean existsByPetIdAndDate(Long petId, LocalDate date);

    Optional<DailyLog> findByExternalIdAndDeletedAtIsNull(String externalId);
    List<DailyLog> findByPetIdAndDeletedAtIsNull(Long petId);
    boolean existsByPetIdAndDateAndDeletedAtIsNull(Long petId, LocalDate date);

    // 대시보드 집계용. created_at 기준(활성 판정)과 date 기준(누적/streak)을 각각 지원한다.
    List<DailyLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime endExclusive);
    List<DailyLog> findByDateBetween(LocalDate start, LocalDate endInclusive);
    List<DailyLog> findByPetIdInAndDateBetween(Collection<Long> petIds, LocalDate start, LocalDate endInclusive);
    List<DailyLog> findByPetIdIn(Collection<Long> petIds);
}
