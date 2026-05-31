package com.kkori.api.photo.service;

import com.kkori.api.auth.context.AuthContext;
import com.kkori.api.auth.context.AuthenticatedUser;
import com.kkori.api.caregiver.entity.Caregiver;
import com.kkori.api.caregiver.repository.CaregiverRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.event.PetImageCleanupEvent;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.photo.dto.request.CreateDailyPhotoRequest;
import com.kkori.api.photo.dto.request.UpdateDailyPhotoRequest;
import com.kkori.api.photo.dto.response.DailyPhotoResponse;
import com.kkori.api.photo.dto.response.DailyPhotoShareResponse;
import com.kkori.api.photo.dto.response.PhotoUploadResponse;
import com.kkori.api.photo.entity.DailyPhoto;
import com.kkori.api.photo.repository.DailyPhotoRepository;
import com.kkori.api.photo.storage.S3PhotoStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPhotoService {

    private final DailyPhotoRepository dailyPhotoRepository;
    private final PetRepository petRepository;
    private final CaregiverRepository caregiverRepository;
    private final DeviceRepository deviceRepository;
    private final S3PhotoStorage s3PhotoStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DailyPhotoResponse create(String deviceExternalId, CreateDailyPhotoRequest request) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        Pet pet = petRepository.findByExternalIdAndDeletedAtIsNull(request.petExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device);

        Caregiver caregiver = caregiverRepository.findByExternalId(request.caregiverExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_001));

        if (dailyPhotoRepository.existsByPetIdAndDateAndDeletedAtIsNull(pet.getId(), request.date())) {
            throw new BusinessException(ErrorCode.PHOTO_002);
        }

        String externalId = resolveExternalId(request.externalId());
        if (request.externalId() != null && !request.externalId().isBlank()
                && dailyPhotoRepository.existsByExternalId(externalId)) {
            throw new BusinessException(ErrorCode.PHOTO_003);
        }

        DailyPhoto photo = DailyPhoto.builder()
                .externalId(externalId)
                .petId(pet.getId())
                .caregiverId(caregiver.getId())
                .date(request.date())
                .caption(request.caption())
                .build();

        return DailyPhotoResponse.from(dailyPhotoRepository.save(photo));
    }

    public List<DailyPhotoResponse> findByPet(String deviceExternalId, String petExternalId) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        Pet pet = petRepository.findByExternalIdAndDeletedAtIsNull(petExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device);

        return dailyPhotoRepository.findByPetIdAndDeletedAtIsNull(pet.getId()).stream()
                .map(DailyPhotoResponse::from)
                .toList();
    }

    public DailyPhotoResponse findByExternalId(String deviceExternalId, String externalId) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        DailyPhoto photo = dailyPhotoRepository.findByExternalIdAndDeletedAtIsNull(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        verifyPetOwnershipById(photo.getPetId(), device);
        return DailyPhotoResponse.from(photo);
    }

    public DailyPhotoShareResponse findShareByExternalId(String externalId) {
        DailyPhoto photo = dailyPhotoRepository.findByExternalIdAndDeletedAtIsNull(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        Pet pet = petRepository.findById(photo.getPetId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        return DailyPhotoShareResponse.of(photo, pet);
    }

    @Transactional
    public DailyPhotoResponse update(String deviceExternalId, String externalId, UpdateDailyPhotoRequest request) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        DailyPhoto photo = dailyPhotoRepository.findByExternalIdAndDeletedAtIsNull(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        verifyPetOwnershipById(photo.getPetId(), device);
        photo.update(request.caption());
        return DailyPhotoResponse.from(photo);
    }

    @Transactional
    public PhotoUploadResponse uploadPhotos(String deviceExternalId, String externalId,
                                            MultipartFile medium, MultipartFile thumbnail) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        DailyPhoto photo = dailyPhotoRepository.findByExternalIdAndDeletedAtIsNull(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        verifyPetOwnershipById(photo.getPetId(), device);

        Pet pet = petRepository.findById(photo.getPetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));

        String mediumUrl = s3PhotoStorage.uploadMedium(pet.getExternalId(), externalId, medium);
        String thumbnailUrl = s3PhotoStorage.uploadThumbnail(pet.getExternalId(), externalId, thumbnail);

        photo.updateUrls(mediumUrl, thumbnailUrl);

        return new PhotoUploadResponse(mediumUrl, thumbnailUrl);
    }

    @Transactional
    public void delete(String deviceExternalId, String externalId) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        DailyPhoto photo = dailyPhotoRepository.findByExternalIdAndDeletedAtIsNull(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        verifyPetOwnershipById(photo.getPetId(), device);

        Pet pet = petRepository.findById(photo.getPetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));

        dailyPhotoRepository.delete(photo);

        if (photo.getMediumUrl() != null) {
            eventPublisher.publishEvent(new PetImageCleanupEvent(
                    List.of(new PetImageCleanupEvent.ImageKey(pet.getExternalId(), externalId))));
        }
    }

    private Device resolveDeviceForRequest(String deviceExternalId) {
        if (AuthContext.currentUser().isPresent()) {
            return resolveDevice(deviceExternalId).orElse(null);
        }
        return resolveDevice(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
    }

    private Optional<Device> resolveDevice(String deviceExternalId) {
        if (deviceExternalId == null || deviceExternalId.isBlank()) {
            return Optional.empty();
        }
        return deviceRepository.findByExternalId(deviceExternalId);
    }

    private void verifyPetOwnership(Pet pet, Device device) {
        Optional<AuthenticatedUser> currentUser = AuthContext.currentUser();
        if (currentUser.isPresent() && pet.getUserId() != null && pet.getUserId().equals(currentUser.get().userId())) {
            return;
        }
        if (device != null && device.getUserId() != null && pet.getUserId() != null && pet.getUserId().equals(device.getUserId())) {
            return;
        }
        if (pet.getUserId() != null || device == null || pet.getDeviceId() == null || !pet.getDeviceId().equals(device.getId())) {
            throw new BusinessException(ErrorCode.PET_001);
        }
    }

    private void verifyPetOwnershipById(Long petId, Device device) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device);
    }

    private String resolveExternalId(String externalId) {
        return (externalId == null || externalId.isBlank())
                ? UUID.randomUUID().toString()
                : externalId;
    }
}
