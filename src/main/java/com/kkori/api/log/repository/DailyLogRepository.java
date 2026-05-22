package com.kkori.api.log.repository;

import com.kkori.api.log.entity.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
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
}
