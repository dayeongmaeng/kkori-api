package com.kkori.api.caregiver.repository;

import com.kkori.api.caregiver.entity.Caregiver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaregiverRepository extends JpaRepository<Caregiver, Long> {
    Optional<Caregiver> findByExternalId(String externalId);
    List<Caregiver> findByDeviceId(Long deviceId);
    boolean existsByExternalIdAndDeviceId(String externalId, Long deviceId);
}
