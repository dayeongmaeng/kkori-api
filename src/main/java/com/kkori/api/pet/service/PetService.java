package com.kkori.api.pet.service;

import com.kkori.api.auth.context.AuthContext;
import com.kkori.api.auth.context.AuthenticatedUser;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.log.entity.DailyLog;
import com.kkori.api.log.repository.DailyLogPhotoRepository;
import com.kkori.api.log.repository.DailyLogRepository;
import com.kkori.api.pet.dto.request.CreatePetRequest;
import com.kkori.api.pet.dto.request.UpdatePetRequest;
import com.kkori.api.pet.dto.response.PetResponse;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.event.PetImageCleanupEvent;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.photo.repository.DailyPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;
    private final DeviceRepository deviceRepository;
    private final DailyLogRepository dailyLogRepository;
    private final DailyLogPhotoRepository dailyLogPhotoRepository;
    private final DailyPhotoRepository dailyPhotoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PetResponse create(String deviceExternalId, CreatePetRequest request) {
        Optional<AuthenticatedUser> currentUser = AuthContext.currentUser();
        Device device = currentUser.isPresent()
                ? resolveDevice(deviceExternalId).orElse(null)
                : requireDevice(deviceExternalId);

        String externalId = resolveExternalId(request.externalId());
        if (request.externalId() != null && !request.externalId().isBlank()
                && petRepository.existsByExternalId(externalId)) {
            throw new BusinessException(ErrorCode.PET_002);
        }

        Pet pet = Pet.builder()
                .externalId(externalId)
                .deviceId(device == null ? null : device.getId())
                .userId(currentUser.map(AuthenticatedUser::userId)
                        .orElse(device == null ? null : device.getUserId()))
                .name(request.name())
                .species(request.species())
                .gender(request.gender())
                .breed(request.breed())
                .birthDate(request.birthDate())
                .birthDateUnknown(request.birthDateUnknown())
                .adoptionDate(request.adoptionDate())
                .weightKg(request.weightKg())
                .neutered(request.neutered())
                .medicalNotes(request.medicalNotes())
                .photoBase64(request.photoBase64())
                .build();
        return PetResponse.from(petRepository.save(pet));
    }

    public List<PetResponse> findAll(String deviceExternalId) {
        Optional<AuthenticatedUser> currentUser = AuthContext.currentUser();
        if (currentUser.isPresent()) {
            return petRepository.findByUserIdAndDeletedAtIsNull(currentUser.get().userId()).stream()
                    .map(PetResponse::from)
                    .toList();
        }
        Device device = requireDevice(deviceExternalId);
        if (device.getUserId() != null) {
            return petRepository.findByUserIdAndDeletedAtIsNull(device.getUserId()).stream()
                    .map(PetResponse::from)
                    .toList();
        }
        return petRepository.findByDeviceIdAndDeletedAtIsNull(device.getId()).stream()
                .map(PetResponse::from)
                .toList();
    }

    public PetResponse findByExternalId(String deviceExternalId, String externalId) {
        Device device = resolveDevice(deviceExternalId).orElse(null);
        return findOwnedPet(externalId, device)
                .map(PetResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
    }

    @Transactional
    public PetResponse update(String deviceExternalId, String externalId, UpdatePetRequest request) {
        Device device = resolveDevice(deviceExternalId).orElse(null);
        Pet pet = findOwnedPet(externalId, device)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        pet.update(request.name(), request.species(), request.gender(), request.breed(), request.birthDate(),
                request.birthDateUnknown(), request.adoptionDate(),
                request.weightKg(), request.neutered(), request.medicalNotes(), request.photoBase64());
        return PetResponse.from(pet);
    }

    @Transactional
    public void delete(String deviceExternalId, String externalId) {
        Device device = resolveDevice(deviceExternalId).orElse(null);
        Pet pet = findOwnedPet(externalId, device)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));

        List<PetImageCleanupEvent.ImageKey> imageKeys = new ArrayList<>();

        List<DailyLog> logs = dailyLogRepository.findByPetIdAndDeletedAtIsNull(pet.getId());
        for (DailyLog log : logs) {
            dailyLogPhotoRepository.findByDailyLogIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(log.getId())
                    .forEach(photo -> {
                        photo.softDelete();
                        imageKeys.add(new PetImageCleanupEvent.ImageKey(pet.getExternalId(), photo.getExternalId()));
                    });
            log.softDelete();
        }
        dailyPhotoRepository.findByPetIdAndDeletedAtIsNull(pet.getId())
                .forEach(photo -> {
                    photo.softDelete();
                    if (photo.getMediumUrl() != null) {
                        imageKeys.add(new PetImageCleanupEvent.ImageKey(pet.getExternalId(), photo.getExternalId()));
                    }
                });

        pet.softDelete();

        if (!imageKeys.isEmpty()) {
            eventPublisher.publishEvent(new PetImageCleanupEvent(imageKeys));
        }
    }

    private Optional<Pet> findOwnedPet(String externalId, Device device) {
        Optional<AuthenticatedUser> currentUser = AuthContext.currentUser();
        if (currentUser.isPresent()) {
            return petRepository.findByExternalIdAndUserIdAndDeletedAtIsNull(externalId, currentUser.get().userId())
                    .or(() -> device == null
                            ? Optional.empty()
                            : petRepository.findByExternalIdAndDeviceIdAndUserIdIsNullAndDeletedAtIsNull(externalId, device.getId()));
        }
        if (device != null && device.getUserId() != null) {
            return petRepository.findByExternalIdAndUserIdAndDeletedAtIsNull(externalId, device.getUserId())
                    .or(() -> petRepository.findByExternalIdAndDeviceIdAndDeletedAtIsNull(externalId, device.getId()));
        }
        if (device == null) {
            return Optional.empty();
        }
        return petRepository.findByExternalIdAndDeviceIdAndDeletedAtIsNull(externalId, device.getId());
    }

    private Device requireDevice(String deviceExternalId) {
        return resolveDevice(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
    }

    private Optional<Device> resolveDevice(String deviceExternalId) {
        if (deviceExternalId == null || deviceExternalId.isBlank()) {
            return Optional.empty();
        }
        return deviceRepository.findByExternalId(deviceExternalId);
    }

    private String resolveExternalId(String externalId) {
        return (externalId == null || externalId.isBlank())
                ? UUID.randomUUID().toString()
                : externalId;
    }
}
