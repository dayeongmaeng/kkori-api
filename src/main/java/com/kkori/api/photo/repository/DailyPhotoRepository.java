package com.kkori.api.photo.repository;

import com.kkori.api.photo.entity.DailyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
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
}
