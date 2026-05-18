package com.kkori.api.log.repository;

import com.kkori.api.log.entity.DailyLogPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyLogPhotoRepository extends JpaRepository<DailyLogPhoto, Long> {
    Optional<DailyLogPhoto> findByExternalId(String externalId);
    List<DailyLogPhoto> findByDailyLogIdOrderBySortOrderAscIdAsc(Long dailyLogId);
    void deleteByDailyLogId(Long dailyLogId);
}
