package com.kkori.api.pet.repository;

import com.kkori.api.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
